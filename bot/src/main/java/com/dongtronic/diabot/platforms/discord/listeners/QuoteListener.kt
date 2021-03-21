package com.dongtronic.diabot.platforms.discord.listeners

import cloud.commandframework.CommandManager
import cloud.commandframework.arguments.StaticArgument
import cloud.commandframework.context.CommandContext
import cloud.commandframework.internal.CommandInputTokenizer
import cloud.commandframework.jda.JDA4CommandManager
import com.dongtronic.diabot.data.mongodb.QuoteDAO
import com.dongtronic.diabot.data.mongodb.QuoteDTO
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.litote.kmongo.eq
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.function.BiConsumer
import java.util.function.Consumer

class QuoteListener(
        private val commandManager: JDA4CommandManager<JDACommandUser>,
        private val updateHandler: JDACommandUpdateHandler? = null
) : ListenerAdapter() {
    // https://emojiguide.org/speech-balloon
    private val speechEmoji = "U+1f4ac"
    private val logger = logger()

    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot) return
        if (!event.reaction.reactionEmote.isEmoji) return
        if (event.reaction.reactionEmote.asCodepoints != speechEmoji) return
        if (!QuoteDAO.checkRestrictions(event.channel)) return

        val author = event.member
        val guild = event.guild

        val messageRetrieval = Mono.defer {
            // defer to prevent retrieving message until subscribe
            event.channel.retrieveMessageById(event.messageId)
                    .submit().toMono()
        }

        /**
         * Replies to the channel only if the reacting user has permission to send messages
         */
        val reply: (message: String) -> Unit = {
            if (event.channel.canTalk(event.member)) {
                event.channel.sendMessage(it).queue()
            }
        }

        val quoteMessage = Consumer<Message> { message ->
            QuoteDAO.getInstance().addQuote(QuoteDTO(
                    guildId = guild.id,
                    channelId = event.channel.id,
                    author = message.author.name,
                    authorId = message.author.id,
                    message = message.contentRaw,
                    messageId = message.id
            )).subscribe({
                message.addReaction(speechEmoji).queue()
                reply("New quote added by ${author.effectiveName} as #${it.quoteId}")
            }, {
                reply("Could not create quote for message: ${message.id}")
            })
        }

        QuoteDAO.getInstance()
                // search for any quotes with this message ID
                .getQuotes(guild.id, QuoteDTO::messageId eq event.messageId)
                .toMono()
                .subscribe(null, { error ->
                    // if there are no quotes then proceed
                    if (error is NoSuchElementException) {
                        messageRetrieval
                                .filter { it.type == MessageType.DEFAULT && !it.author.isBot }
                                .subscribe(quoteMessage)
                    }
                })
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot || !event.isFromGuild) return

        val msg = event.message.contentRaw
        val quoteLiteral = StaticArgument.of<JDACommandUser>("quote", "q")

        if (msg.startsWith(".")) {
            val fullCommand = msg.substringAfter('.')
            val tokenised = CommandInputTokenizer(fullCommand).tokenize()

            val sender = JDACommandUser.of(event, updateHandler)
            val parsed = commandManager.commandTree.parse(CommandContext(sender, commandManager), tokenised)

            if (parsed.first?.arguments?.first() == quoteLiteral) {
                commandManager.executeCommand(sender, fullCommand)
                        .whenComplete { _, throwable ->
                            if (throwable is Exception) {
                                commandManager.getExceptionHandler(throwable)?.accept(sender, throwable)
                            }
                        }
            } else {
                val exception = parsed.second ?: return

                commandManager.getExceptionHandler(exception)?.accept(sender, exception)
            }
        }
    }

    private fun <C, E : Exception> CommandManager<C>.getExceptionHandler(exception: E): BiConsumer<C, E>? {
        // I don't know why Kotlin is having such a hard time with the generics for this, but this function seems
        // to get around the issue.
        @Suppress("UNCHECKED_CAST")
        return this.getExceptionHandler(exception::class.java) as BiConsumer<C, E>?
    }
}