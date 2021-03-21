package com.dongtronic.diabot.platforms.discord.commands.quote

import cloud.commandframework.annotations.*
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.ReplyType
import com.dongtronic.diabot.commands.annotations.*
import com.dongtronic.diabot.data.mongodb.QuoteDAO
import com.dongtronic.diabot.data.mongodb.QuoteDTO
import com.dongtronic.diabot.exceptions.RequestStatusException
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.internal.requests.Requester
import okhttp3.OkHttpClient
import okhttp3.Request
import reactor.core.Exceptions
import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.io.IOException
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong

class QuoteImportCommand : EventListener {
    private val mapper = jacksonObjectMapper()
    private val logger = logger()
    private val pendingRequests = mutableSetOf<User>()
    private var guildMessages = DirectProcessor.create<GuildMessageReceivedEvent>()
    private var eventListening = false

    @Hidden
    @OwnersOnly
    @GuildOnly
    @DiscordPermission(Permission.MESSAGE_MANAGE)
    @CommandMethod("quote import [url]")
    @CommandDescription("Imports UB3R-B0T quotes from a JSON file, either provided via a URL or a file attachment")
    @CommandCategory(Category.FUN)
    @Example(["[import] https://pastebin.com/raw/...", "[import] (with a JSON file attached)"])
    fun execute(
            sender: JDACommandUser,
            @Greedy
            @Argument("url", description = "A URL which provides the UB3R-B0T quotes in JSON form")
            url: String?
    ) {
        val event = sender.event
        registerListener(event.jda)

        if (!QuoteDAO.checkRestrictions(event.textChannel, warnDisabledGuild = true)) return
        if (pendingRequests.contains(event.author)) return
        pendingRequests.add(event.author)

        if (url != null || event.message.attachments.isNotEmpty()) {
            parse(sender, url)
            return
        }

        // create an interactive session:
        // listens for a URL or attachment from the user's next message
        guildMessages.filter { it.author == event.author && it.channel == event.channel && it.guild == event.guild }
                .map { it.message }
                .toMono()
                .timeout(Duration.ofSeconds(90))
                .subscribeOn(Schedulers.parallel())
                .doOnSubscribe {
                    // send the initial message
                    sender.replyS("Type a URL, attach a file, or type `cancel`", ReplyType.MENTION)
                }
                .subscribe({
                    if (it.contentStripped == "cancel") {
                        pendingRequests.remove(event.author)
                        sender.replySuccessS("Import cancelled", ReplyType.MENTION)
                        return@subscribe
                    }

                    parse(sender, it.contentDisplay, it)
                }, {
                    pendingRequests.remove(event.author)
                    if (it is TimeoutException) {
                        sender.replyWarningS("Import timed out", ReplyType.MENTION)
                    }
                })
    }

    /**
     * Registers this event listener *once*
     *
     * @param jda The JDA client instance to register under
     */
    private fun registerListener(jda: JDA) {
        if (!eventListening) {
            eventListening = true
            jda.addEventListener(this)
        }
    }

    /**
     * Parses the incoming import request and begins the task of importing.
     *
     * @param event The command event which this request came from.
     * If the request came from interactive mode, this will be the `diabot quote import` event.
     *
     * @param url Optional. The command's arguments, which should consist of a URL if that is being used.
     * If the request came from interactive mode, this will be the second message's contents.
     *
     * @param message Optional. The request's [Message] instance.
     * If the request came from interactive mode, this will be the second message.
     */
    private fun parse(sender: JDACommandUser, url: String?, message: Message = sender.event.message) {
        val event = sender.event
        val successful = AtomicLong()
        val failed = AtomicLong()
        val startTime = System.currentTimeMillis()

        val importQuotes = if (message.attachments.isNotEmpty()) {
            getAttachmentsText(message.attachments)
        } else {
            getUrlText(url!!)
        }.subscribeOn(Schedulers.boundedElastic())
                // Delete the message which has the file/attachment once retrieved and indicate the task started
                .doFinally {
                    // if we delete their first message then send a new message
                    if (message == event.message) {
                        sender.replyS("Starting import task..", ReplyType.MENTION)
                    } else {
                        sender.reactSuccessS()
                    }

                    message.delete().queue()
                }
                // Map from JSON to data class
                .flatMapMany { mapper.readValue<List<Ub3rQuote>>(it).toFlux() }
                // Upsert
                .flatMap {
                    if (it.server.isNotBlank() && it.server != event.guild.id) {
                        return@flatMap Mono.error<QuoteDTO>(IllegalArgumentException("Quotes must be from the current server"))
                    }

                    val diabotQuote = it.toDiabotQuote(event.guild)
                    upsertQuote(diabotQuote)
                            // Used for final statistics
                            .doOnNext { successful.getAndIncrement() }
                            .onErrorContinue { throwable, quote ->
                                failed.getAndIncrement()
                                logger.warn("Could not import $quote", throwable)
                            }
                }
                // Unwrap throwables from `ReactiveException`
                .onErrorMap(Exceptions::unwrap)
                // Remove user from the pending requests once all quotes have been processed
                .doFinally { pendingRequests.remove(message.author) }

        importQuotes.subscribe({
            // on each
            logger.debug("Finished adding $it")
        }, {
            // on error
            var errorMessage = "Import failed: ${it::class.simpleName} - ${it.message}"
            when (it) {
                is IllegalArgumentException -> {
                    errorMessage = "Invalid argument: ${it.message}"
                }
                is MalformedURLException,
                is URISyntaxException -> {
                    errorMessage = "Provided URL was invalid"
                    logger.warn("Invalid URL: " + it::class.simpleName + " - " + it.message)
                }
                is IOException -> {
                    errorMessage = "Could not load input"
                    logger.warn("IO error: " + it::class.simpleName + " - " + it.message)
                }
                else -> {
                    logger.warn("Unexpected error: " + it::class.simpleName + " - " + it.message + " - " + it.stackTrace)
                }
            }

            sender.replyError(errorMessage, ReplyType.MENTION)
        }, {
            // on finish
            val totalTime = System.currentTimeMillis() - startTime
            sender.replySuccessS("Finished importing quotes: ${successful.get()} successful, ${failed.get()} " +
                    "unsuccessful in $totalTime ms.",
                    ReplyType.MENTION)
        })
    }

    /**
     * Inserts a quote into the database if it does not exist, otherwise it will update the existing quote.
     *
     * @param quoteDTO The quote to upsert
     * @return The quote which was upserted
     */
    private fun upsertQuote(quoteDTO: QuoteDTO): Mono<QuoteDTO> {
        val quoteDAO = QuoteDAO.getInstance()
        val filter = QuoteDAO.filter(quoteDTO.guildId, quoteDTO.quoteId)
        var query = quoteDAO.collection.countDocuments(filter).toMono()

        if (quoteDTO.quoteId == null) {
            // skip getting count as there will be no matches
            query = Mono.just(0L)
        }

        return query.flatMap { count ->
            if (count == 0L) {
                // if there's no quotes matching this ID then add it
                return@flatMap quoteDAO.addQuote(quoteDTO)
            }

            // otherwise, update the quote with the matching ID
            quoteDAO.updateQuote(quoteDTO).flatMap {
                if (it.matchedCount == 0L) {
                    Mono.error(NoSuchElementException())
                } else {
                    quoteDTO.toMono()
                }
            }
        }
    }

    /**
     * Gets an attachment with a `.json` filename and returns its text
     *
     * @param attachments A list of the attachments
     * @return The JSON attachment's contents, if any
     */
    @Suppress("ReactiveStreamsUnusedPublisher")
    private fun getAttachmentsText(attachments: List<Message.Attachment>): Mono<String> {
        val attachment = attachments.firstOrNull { it.fileName.endsWith(".json") }?.url

        return if (attachment != null)
            getUrlText(attachment)
        else
            Mono.error(IllegalArgumentException("Could not open attachment"))
    }

    /**
     * Grabs the URL's contents
     *
     * @param url The URL to grab contents of
     * @return The contents of the URL
     */
    private fun getUrlText(url: String): Mono<String> {
        return Mono.fromCallable {
            val client = OkHttpClient()
            val request = Request.Builder()
                    .get()
                    .url(URL(url))
                    .addHeader("user-agent", Requester.USER_AGENT)
                    .build()

            client.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    throw RequestStatusException(response.code)
                }

                body.string()
            }
        }
    }

    /**
     * Listens to incoming messages for the interactive mode
     */
    override fun onEvent(event: GenericEvent) {
        if (event !is GuildMessageReceivedEvent) return

        if (event.author.isBot) return

        guildMessages.onNext(event)
    }

    /**
     * A data class representing UB3R-B0T quotes in JSON form
     */
    @JsonAutoDetect
    data class Ub3rQuote(
            val id: String,
            val nick: String,
            val userId: String,
            val channel: String? = null,
            val server: String,
            val text: String,
            val messageId: String,
            val time: Long,
            val dateTime: String
    ) {
        /**
         * Converts this UB3R-B0T quote into a Diabot [QuoteDTO]
         *
         * @param guild The guild which this quote belongs to. This is used for looking up channel and guild IDs.
         * May be null, however the channel IDs will be blank
         * @return Converted quote object
         */
        fun toDiabotQuote(guild: Guild? = null): QuoteDTO {
            // looks up the channel name and tries to get its id
            // the channel key can be missing, which is why we need a null check
            val channelId = if (channel != null) {
                guild?.textChannels?.firstOrNull { it.name == channel }?.id ?: ""
            } else ""

            return QuoteDTO(quoteId = id,
                    guildId = server,
                    channelId = channelId,
                    author = nick,
                    authorId = userId,
                    message = text,
                    messageId = messageId,
                    time = time)
        }
    }
}