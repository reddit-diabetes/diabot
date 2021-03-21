package com.dongtronic.diabot.platforms.discord.commands.nightscout

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.ReplyType
import com.dongtronic.diabot.commands.annotations.ChildCommands
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DisplayName
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.data.mongodb.ChannelDAO
import com.dongtronic.diabot.data.mongodb.ChannelDTO
import com.dongtronic.diabot.data.mongodb.NightscoutDAO
import com.dongtronic.diabot.data.mongodb.NightscoutUserDTO
import com.dongtronic.diabot.exceptions.NightscoutDataException
import com.dongtronic.diabot.exceptions.NightscoutPrivateException
import com.dongtronic.diabot.exceptions.NightscoutStatusException
import com.dongtronic.diabot.exceptions.UnconfiguredNightscoutException
import com.dongtronic.diabot.logic.diabetes.BGConversionFormatter
import com.dongtronic.diabot.nameOf
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutDisplayCommands.DisplayOptions.*
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutDisplayCommands.DisplayOptions.Companion.sortOptions
import com.dongtronic.diabot.util.logger
import com.dongtronic.nightscout.Nightscout
import com.dongtronic.nightscout.data.NightscoutDTO
import com.dongtronic.nightscout.exceptions.NoNightscoutDataException
import com.fasterxml.jackson.core.JsonProcessingException
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.onErrorMap
import reactor.kotlin.core.publisher.onErrorResume
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.util.function.Tuple2
import retrofit2.HttpException
import java.awt.Color
import java.net.UnknownHostException
import java.time.Instant
import java.time.temporal.ChronoUnit

@ChildCommands(NightscoutSubcommands::class, NightscoutDisplayCommands::class)
class NightscoutCommand {

    private val logger = logger()

    @CommandMethod("nightscout|ns|bg|bs [source]")
    @CommandDescription("Get the most recent info from any Nightscout site")
    @CommandCategory(Category.BG)
    @Example(["[nightscout] casscout", "[ns]", "[ns] set https://casscout.herokuapp.com", "[ns] @SomeUser#1234", "[ns] public false"])
    fun execute(
            user: JDACommandUser,
            @Argument("source", description = "The source of BG data")
            @DisplayName("nightscout URL/herokuapp subdomain/discord user/mention")
            @Greedy
            arg: String?
    ) {
        val args = arg?.trim() ?: ""

        // grab the necessary data
        val embed = if (args.isBlank()) {
            // read NS data from the author's NS
            getStoredData(user)
        } else {
            // parse the given arguments
            getUnstoredData(user, args)
        }.flatMap { data ->
            // send the message
            user.reply(data.t2, ReplyType.NONE)
                    .doOnSuccess { addReactions(data.t1, it) }
        }.subscribeOn(Schedulers.boundedElastic())

        embed.subscribe({
            logger.debug("Sent Nightscout embed: $it")
        }, {
            handleError(it, user)
        })
    }

    /**
     * Handles errors which occur either:
     * - before fetching data from a Nightscout instance
     * or
     * - when replying
     *
     * @param ex The error which was thrown
     * @param user The command sender which called this command
     */
    private fun handleError(ex: Throwable, user: JDACommandUser) {
        val response = when (ex) {
            is NightscoutDataException -> {
                if (ex.message != null) {
                    ex.message!!
                } else {
                    "Nightscout data could not be read"
                }
            }
            is UnconfiguredNightscoutException -> "Please set your Nightscout hostname using `diabot nightscout set <hostname>`"
            is IllegalArgumentException -> "Error: " + ex.message
            is InsufficientPermissionException -> {
                logger.info("Couldn't reply with nightscout data due to missing permission: ${ex.permission}")
                "Couldn't perform requested action due to missing permission: `${ex.permission}`"
            }
            is UnknownHostException -> {
                logger.info("No host found: ${ex.message}")
                "No NS host could be found"
            }
            else -> {
                logger.warn("Unexpected error: " + ex.message, ex)
                "Unexpected error occurred"
            }
        }

        user.replyErrorS(response, ReplyType.NONE)
    }

    /**
     * Handles errors which occur while grabbing Nightscout data.
     *
     * @param ex The [Throwable] which was given
     * @param user The user which requested the bot to grab this Nightscout data
     * @param userDTO The user data which was used for fetching
     */
    private fun handleGrabError(ex: Throwable, user: JDACommandUser, userDTO: NightscoutUserDTO) {
        val event = user.event
        val response = when (ex) {
            is NoNightscoutDataException -> {
                logger.info("No nightscout data from ${userDTO.url}")
                "No data could be retrieved from Nightscout"
            }
            is JsonProcessingException -> {
                logger.warn("Malformed JSON from ${userDTO.url}")
                "Malformed JSON"
            }
            is NightscoutStatusException -> {
                if (ex.status == 401) {
                    if (userDTO.jdaUser != null) {
                        if (userDTO.jdaUser == event.author) {
                            "Could not authenticate to Nightscout. Please set an authentication token with `diabot nightscout token <token>`"
                        } else {
                            val name = event.nameOf(userDTO.jdaUser)
                            "Nightscout data for $name is unreadable due to missing token."
                        }
                    } else {
                        "Nightscout data is unreadable due to missing token."
                    }
                } else {
                    logger.warn("Connection status ${ex.status} from ${userDTO.url}")
                    "Could not connect to Nightscout instance."
                }
            }
            else -> return
        }

        user.replyErrorS(response, ReplyType.NONE)
    }

    /**
     * Grabs data for the command sender and builds a Nightscout response.
     *
     * @param sender The user which called this command
     * @return A nightscout DTO and an embed based on it
     */
    private fun getStoredData(sender: JDACommandUser): Mono<Tuple2<NightscoutDTO, MessageEmbed>> {
        return getUserDto(sender.event.author)
                .flatMap { buildNightscoutResponse(it, sender) }
    }

    /**
     * Grabs data for another user/URL (depending on the arguments) and builds a Nightscout response.
     *
     * @param sender The user which called this command
     * @return A nightscout DTO and an embed based on it
     */
    private fun getUnstoredData(sender: JDACommandUser, args: String): Mono<Tuple2<NightscoutDTO, MessageEmbed>> {
        val event = sender.event
        val guild = if (event.isFromGuild) event.guild else null
        val args1 = args.split(' ')

        val namedMembers =
                guild?.members?.filter {
                    it.effectiveName.equals(args, true)
                            || it.user.name.equals(args, true)
                } ?: emptyList()
        val mentionedMembers = guild?.let { event.message.getMentionedMembers(it) } ?: emptyList()

        val endpoint: Mono<NightscoutUserDTO> = when {
            mentionedMembers.size > 1 ->
                IllegalArgumentException("Too many mentioned users.").toMono()

            event.message.mentionsEveryone() ->
                IllegalArgumentException("Cannot handle mentioning everyone.").toMono()

            mentionedMembers.size == 1 -> {
                val member = mentionedMembers.first()
                getUserDtoPublic(member)
            }

            args1.isNotEmpty() && args1[0].matches("^https?://.*".toRegex()) -> {
                // is a URL
                val url = NightscoutSubcommands.validateNightscoutUrl(args1[0])
                getDataFromDomain(url, event)
            }

            else -> {
                // Try to get nightscout data from username/nickname, otherwise just try to get from hostname
                val member = namedMembers.firstOrNull()
                val domain = "https://${args1[0]}.herokuapp.com"
                val fallbackDto = NightscoutUserDTO(url = domain).toMono()

                if (member == null) {
                    fallbackDto
                } else {
                    getUserDtoPublic(member).onErrorResume(IllegalArgumentException::class) { fallbackDto }
                }
            }
        }

        return endpoint.flatMap { buildNightscoutResponse(it, sender) }
    }

    /**
     * Grabs user data for a Nightscout domain.
     *
     * @param domain The domain to look up
     * @param event Command event which called this command
     * @return NS user DTO for this domain. This will be a generic DTO if there was no data found.
     */
    private fun getDataFromDomain(domain: String, event: MessageReceivedEvent): Mono<NightscoutUserDTO> {
        val userDtos = getUsersForDomain(domain)
        // temporary userDTO for if this domain does not belong to anyone in the guild
        val fallback = NightscoutUserDTO(url = domain).toMono()

        return userDtos
                .flatMap { userDTO ->
                    val member = event.guild.getMemberById(userDTO.userId)
                    val publicInThisGuild = userDTO.isNightscoutPublic(event.guild.id)

                    if (member == null) {
                        // use generic DTO if the user is not in the guild which we are replying in.
                        // this is to prevent users in other guilds from being able to see whether a NS belongs to any diabot user
                        return@flatMap fallback
                    }

                    val user = member.user

                    if (!publicInThisGuild) {
                        return@flatMap NightscoutPrivateException(event.nameOf(user))
                                .toMono<NightscoutUserDTO>()
                    }

                    userDTO.copy(jdaUser = user).toMono()
                }
                .singleOrEmpty()
                .switchIfEmpty { fallback }
    }

    /**
     * Loads all the necessary data from a Nightscout instance and creates an embed of it.
     *
     * @param userDTO Data necessary for loading/rendering
     * @param sender The user which called this command
     * @return A nightscout DTO and an embed based on it
     */
    private fun buildNightscoutResponse(userDTO: NightscoutUserDTO, sender: JDACommandUser): Mono<Tuple2<NightscoutDTO, MessageEmbed>> {
        val event = sender.event
        val api = Nightscout(userDTO.apiEndpoint, userDTO.token)

        return Mono.from(
                api.getSettings()
                        .flatMap { api.getRecentSgv(it) }
                        .flatMap { api.getPebble(it) }
        ).doFinally {
            api.close()
        }.onErrorMap(HttpException::class) {
            NightscoutStatusException(it.code())
        }.onErrorResume(
                { error ->
                    error is NightscoutStatusException
                            || error is JsonProcessingException
                            || error is NoNightscoutDataException
                },
                {
                    // fallback to `handleGrabError` if the error is any of the above
                    handleGrabError(it, sender, userDTO)
                    Mono.empty<NightscoutDTO>()
                }
        ).zipWhen { nsDto ->
            // attach a message embed to the NightscoutDTO
            val channelType = event.channelType
            var isShort = false.toMono()

            if (channelType == ChannelType.TEXT) {
                isShort = if (userDTO.displayOptions.contains(SIMPLE)) {
                    true.toMono()
                } else {
                    ChannelDAO.instance.hasAttribute(event.channel.id, ChannelDTO.ChannelAttribute.NIGHTSCOUT_SHORT)
                }
            }

            isShort.map { buildResponse(nsDto, userDTO, it).build() }
        }
    }

    /**
     * Builds an embed from NS data.
     *
     * @param nsDTO The Nightscout data to build a response off of
     * @param userDTO The owner of the [NightscoutDTO] data
     * @param short Whether to exclude the user avatar from this embed
     * @param builder Optional: the embed to build off of
     * @return The embed which was created
     */
    private fun buildResponse(
            nsDTO: NightscoutDTO,
            userDTO: NightscoutUserDTO,
            short: Boolean,
            builder: EmbedBuilder = EmbedBuilder()
    ): EmbedBuilder {
        val newest = nsDTO.getNewestEntry()
        val trendString = newest.trend.unicode
        val displayOptions = userDTO.displayOptions

        val (mmolString: String, mgdlString: String) = buildGlucoseStrings(nsDTO)
        builder.addField("mmol/L", mmolString, true)
        builder.addField("mg/dL", mgdlString, true)

        displayOptions.sortOptions().forEach {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (it) {
                TITLE -> builder.setTitle(nsDTO.title)
                TREND -> builder.addField("trend", trendString, true)
                COB -> {
                    if (nsDTO.iob != 0.0F) {
                        builder.addField("iob", nsDTO.iob.toString(), true)
                    }
                }
                IOB -> {
                    if (nsDTO.cob != 0) {
                        builder.addField("cob", nsDTO.cob.toString(), true)
                    }
                }
                AVATAR -> {
                    if (!short && userDTO.jdaUser?.avatarUrl != null) {
                        builder.setThumbnail(userDTO.jdaUser.avatarUrl)
                    }
                }
            }
        }

        setResponseColor(nsDTO, builder)

        builder.setTimestamp(newest.dateTime)
        builder.setFooter("measured", "https://github.com/nightscout/cgm-remote-monitor/raw/master/static/images/large.png")

        if (newest.dateTime.plus(15, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            builder.setDescription("**BG data is more than 15 minutes old**")
        }

        return builder
    }

    /**
     * Builds glucose strings from [NightscoutDTO] into mmol/L and mg/dL values, including delta if there is any.
     *
     * @param dto The Nightscout DTO to read glucose from
     * @return A pair consisting of mmol/L in the first value and mg/dL in the second value
     */
    private fun buildGlucoseStrings(dto: NightscoutDTO): Pair<String, String> {
        val newest = dto.getNewestEntry()
        val mmolString: String
        val mgdlString: String
        if (newest.delta != null) {
            mmolString = buildGlucoseString(newest.glucose.mmol.toString(), newest.delta!!.mmol.toString())
            mgdlString = buildGlucoseString(newest.glucose.mgdl.toString(), newest.delta!!.mgdl.toString())
        } else {
            mmolString = buildGlucoseString(newest.glucose.mmol.toString(), "999.0")
            mgdlString = buildGlucoseString(newest.glucose.mgdl.toString(), "999.0")
        }
        return Pair(mmolString, mgdlString)
    }

    /**
     * Formats a glucose value and delta (if available)
     *
     * @param glucose The glucose value.
     * @param delta The current delta.
     * @return Formatted glucose and delta
     */
    private fun buildGlucoseString(glucose: String, delta: String): String {
        val builder = StringBuilder()

        builder.append(glucose)

        if (delta != "999.0") {
            // 999L is placeholder for absent delta
            builder.append(" (")

            if (!delta.startsWith("-")) {
                builder.append("+")
            }

            builder.append(delta)
            builder.append(")")
        }

        return builder.toString()
    }

    /**
     * Adds reactions to a message based on the glucose value.
     *
     * @param dto The Nightscout DTO holding the glucose data.
     * @param response The message to react to.
     */
    private fun addReactions(dto: NightscoutDTO, response: Message) {
        val newest = dto.getNewestEntry()

        BGConversionFormatter.getReactions(newest.glucose).forEach {
            response.addReaction(it).queue()
        }
    }

    /**
     * Adjust an embed's color based on the current glucose and ranges.
     *
     * @param dto The Nightscout DTO holding the glucose and range data.
     * @param builder Optional: the embed to set colors on.
     */
    private fun setResponseColor(dto: NightscoutDTO, builder: EmbedBuilder = EmbedBuilder()): EmbedBuilder {
        val glucose = dto.getNewestEntry().glucose.mgdl.toDouble()

        if (glucose >= dto.high || glucose <= dto.low) {
            builder.setColor(Color.red)
        } else if (glucose >= dto.top && glucose < dto.high || glucose > dto.low && glucose <= dto.bottom) {
            builder.setColor(Color.orange)
        } else {
            builder.setColor(Color.green)
        }

        return builder
    }

    /**
     * Finds users in the database matching with the Nightscout domain given
     *
     * @param domain The domain to look up
     * @return The [NightscoutUserDTO]s which match the given domain
     */
    private fun getUsersForDomain(domain: String): Flux<NightscoutUserDTO> {
        return NightscoutDAO.instance.getUsersForURL(domain)
                // if there are no users then return an empty Flux
                .onErrorResume(NoSuchElementException::class) { Flux.empty() }
    }

    /**
     * Gets a [NightscoutUserDTO] for the given user.
     *
     * @param user The user to look up
     * @param throwable The exception to throw if the user has not configured their Nightscout
     * @return A [NightscoutUserDTO] instance belonging to the given user
     */
    private fun getUserDto(user: User, throwable: Throwable = UnconfiguredNightscoutException()): Mono<NightscoutUserDTO> {
        return NightscoutDAO.instance.getUser(user.id)
                .onErrorMap(NoSuchElementException::class) { throwable }
                .flatMap {
                    if (it.url != null) {
                        it.copy(jdaUser = user).toMono()
                    } else {
                        // throw an exception if the url is blank
                        throwable.toMono()
                    }
                }
    }

    /**
     * Gets a [NightscoutUserDTO] for the given member on another user's behalf. This function checks the member's
     * privacy settings and will throw a [NightscoutPrivateException] if it is private in the guild.
     *
     * @param member Member to retrieve Nightscout data from
     * @param throwable Exception to throw if the member does not have a Nightscout URL configured
     * @return A [NightscoutUserDTO] instance belonging to the given member
     */
    private fun getUserDtoPublic(
            member: Member,
            throwable: Throwable = IllegalArgumentException("User does not have a configured Nightscout URL.")
    ): Mono<NightscoutUserDTO> {
        return getUserDto(member.user, throwable)
                .handle { t, u: SynchronousSink<NightscoutUserDTO> ->
                    if (!t.isNightscoutPublic(member.guild.id)) {
                        u.error(NightscoutPrivateException(member.effectiveName))
                    } else {
                        u.next(t)
                    }
                }
    }
}