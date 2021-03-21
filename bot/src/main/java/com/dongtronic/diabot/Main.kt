package com.dongtronic.diabot

import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext
import cloud.commandframework.jda.JDA4CommandManager
import cloud.commandframework.jda.parsers.UserArgument
import cloud.commandframework.jda.parsers.UserArgument.Isolation
import cloud.commandframework.jda.parsers.UserArgument.UserParser
import com.dongtronic.diabot.commands.DiabotHelp
import com.dongtronic.diabot.commands.DiabotParser
import com.dongtronic.diabot.commands.PermissionRegistry
import com.dongtronic.diabot.commands.ReplyType
import com.dongtronic.diabot.commands.cooldown.CooldownIds
import com.dongtronic.diabot.commands.cooldown.CooldownMeta
import com.dongtronic.diabot.data.migration.MigrationManager
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.platforms.discord.commands.admin.AdminCommands
import com.dongtronic.diabot.platforms.discord.commands.admin.OwnerCommand
import com.dongtronic.diabot.platforms.discord.commands.admin.RolesCommand
import com.dongtronic.diabot.platforms.discord.commands.admin.ShutdownCommand
import com.dongtronic.diabot.platforms.discord.commands.diabetes.ConvertCommand
import com.dongtronic.diabot.platforms.discord.commands.diabetes.EstimationCommand
import com.dongtronic.diabot.platforms.discord.commands.info.AboutCommand
import com.dongtronic.diabot.platforms.discord.commands.info.InfoCommands
import com.dongtronic.diabot.platforms.discord.commands.misc.*
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutAdminCommands
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutCommand
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutGraphCommand
import com.dongtronic.diabot.platforms.discord.commands.quote.QuoteCommand
import com.dongtronic.diabot.platforms.discord.commands.rewards.RewardsCommands
import com.dongtronic.diabot.platforms.discord.listeners.*
import com.dongtronic.diabot.util.logger
import com.github.ygimenez.method.Pages
import com.github.ygimenez.model.PaginatorBuilder
import io.leangen.geantyref.TypeToken
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function
import javax.security.auth.login.LoginException


object Main {
    private val logger = logger()
    val ownerIds = arrayOf("189436077793083392") // Cas
    val coOwnerIds = arrayOf("125616270254014464", "319371513159614464") // Adi, Garlic
    val allOwnerIds = ownerIds.plus(coOwnerIds)

    @Throws(LoginException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Migrate data from Redis to MongoDB
        MigrationManager().migrateIfNecessary()

        val token = System.getenv("DIABOTTOKEN")

        val cmdUpdateHandler = JDACommandUpdateHandler(100)
        val jda = JDABuilder.createLight(token)
                .setEnabledIntents(
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL) // Cache all members regardless of their online state
                .setChunkingFilter(ChunkingFilter.ALL) // Cache all guilds on initialisation
                .disableCache(EnumSet.allOf(CacheFlag::class.java)) // We don't need any JDA cache services
                .build()

        // Pagination
        val paginator = PaginatorBuilder.createPaginator()
                .setHandler(jda)
                .shouldRemoveOnReact(true)
                .build()
        Pages.activate(paginator)

        // sets the bot prefix
        val prefix = if (System.getenv("DIABOT_DEBUG") != null) "dl " else "diabot "

        val permissionRegistry = PermissionRegistry()
        val commandManager: JDA4CommandManager<JDACommandUser> = JDA4CommandManager(
                jda,
                Function { prefix },
                BiFunction { sender: JDACommandUser, permission: String ->
                    permissionRegistry.hasPermission(sender, permission)
                },
                AsynchronousCommandExecutionCoordinator
                        .newBuilder<JDACommandUser>()
                        .withSynchronousParsing()
                        .build(),
                Function {
                    JDACommandUser.of(it, cmdUpdateHandler)
                },
                Function {
                    it.toJdaCommandSender()
                }
        )

        // override default user parser. as of cloud 1.5.0 the default isolation is GLOBAL, but GUILD is usually safer
        commandManager.parserRegistry.registerParserSupplier(TypeToken.get(User::class.java)) {
            UserParser<JDACommandUser>(UserArgument.ParserMode.values().toSet(), Isolation.GUILD)
        }

        // allow global isolation if explicitly specified (by using the parser named `user-global`)
        commandManager.parserRegistry.registerNamedParserSupplier("user-global") {
            UserParser<JDACommandUser>(
                    setOf(
                            UserArgument.ParserMode.ID,
                            UserArgument.ParserMode.MENTION
                    ),
                    Isolation.GLOBAL
            )
        }

        val diabotHelp = DiabotHelp(
                commandManager,
                prefix,
                { sender: JDACommandUser, message: Message ->
                    sender.reply(message, ReplyType.NONE)
                },
                { sender: JDACommandUser, user: User ->
                    user.id == sender.getAuthorUniqueId()
                }
        )

        DiabotParser(commandManager, JDACommandUser::class.java)
                .addAutoPermissionSupport()
                .addDiscordPermissionSupport()
                .addCategorySupport()
                .addExampleSupport()
                .addOwnersOnlySupport(*allOwnerIds) {}
                .addGuildOnlySupport {
                    it.commandContext.sender.replyErrorS("This command can only be executed in a server.")
                }
                .addAdminChannelOnlySupport {
                    it.commandContext.sender.replyErrorS("This command can only be executed in an admin channel.")
                }
                .addHomeGuildOnlySupport {
                    val message = System.getenv()["HOME_GUILD_MESSAGE"]
                            ?: "This command can only be executed in the bot's home guild."

                    it.commandContext.sender.replyErrorS(message)
                }
                .addCooldownSupport(
                        idMapper = {
                            val guildId = if (it.event.isFromGuild) it.event.guild.id else ""
                            CooldownIds(
                                    userId = it.getAuthorUniqueId(),
                                    channelId = it.event.channel.id,
                                    guildId = guildId,
                                    shardId = it.event.jda.shardInfo.shardId.toString()
                            )
                        },
                        notifier = { millisRemaining: Long, _: CooldownMeta, context: CommandPostprocessingContext<JDACommandUser> ->
                            val secsLeft = (millisRemaining / 1000).toInt()
                            val s = if (secsLeft == 1) "" else "s"
                            context.commandContext.sender.replyErrorS("This command is currently in cooldown for $secsLeft more second$s")
                        }
                )
                .parse(arrayOf(
                        // A1c
                        EstimationCommand(),

                        // BG Conversions
                        ConvertCommand(),
                        NightscoutCommand(),
                        NightscoutGraphCommand(),

                        // Utility
                        PingCommand(),
                        RewardsCommands(),
                        GithubCommand(),
                        DisclaimerCommand(),
                        NutritionCommand(),

                        // Info
                        InfoCommands(),
                        SupportCommand(),
                        AboutCommand(prefix),
                        HelpCommand(diabotHelp),

                        // Fun
                        ExcuseCommand(),
                        AwyissCommand(),
                        DiacastCommand(),
                        OwnerCommand(),
                        QuoteCommand(),

                        // Admin
                        AdminCommands(),
                        ShutdownCommand(),
                        NightscoutAdminCommands(),
                        RolesCommand()
                ))

        // add event listeners
        jda.addEventListener(
                ConversionListener(cmdUpdateHandler),
                RewardListener(),
                UsernameEnforcementListener(),
                OhNoListener(),
                QuoteListener(commandManager, cmdUpdateHandler),
                cmdUpdateHandler
        )

        commandManager.commandHelpHandler.allCommands.forEach {
            // debug code
            logger.debug("Syntax: ${it.syntaxString}; Desc: ${it.description}")
        }
    }
}
