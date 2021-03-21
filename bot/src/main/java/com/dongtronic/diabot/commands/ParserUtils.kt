package com.dongtronic.diabot.commands

import cloud.commandframework.Command
import cloud.commandframework.CommandManager
import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.arguments.StaticArgument
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext
import cloud.commandframework.meta.CommandMeta
import cloud.commandframework.permission.AndPermission
import cloud.commandframework.permission.CommandPermission
import cloud.commandframework.permission.OrPermission
import cloud.commandframework.permission.Permission
import cloud.commandframework.services.types.ConsumerService
import com.dongtronic.diabot.commands.annotations.*
import com.dongtronic.diabot.commands.cooldown.CooldownIds
import com.dongtronic.diabot.commands.cooldown.CooldownMeta
import com.dongtronic.diabot.commands.cooldown.CooldownStorage
import com.dongtronic.diabot.data.mongodb.ChannelDAO
import com.dongtronic.diabot.data.mongodb.ChannelDTO
import com.dongtronic.diabot.util.logger
import io.leangen.geantyref.TypeToken
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*
import kotlin.reflect.full.createInstance

/**
 * Utility class which provides features unique to Diabot for the Cloud framework.
 */
@Suppress("MemberVisibilityCanBePrivate")
object ParserUtils {
    private val logger = logger()
    val META_AUTOPERM_ALLOWED = CommandMeta.Key.of(TypeToken.get(Boolean::class.javaObjectType), "autopermission")
    val META_CATEGORY = CommandMeta.Key.of(TypeToken.get(Category::class.java), "category")
    val META_EXAMPLE = CommandMeta.Key.of(TypeToken.get(Array<String>::class.java), "example")
    val META_GUILDONLY = CommandMeta.Key.of(TypeToken.get(Boolean::class.javaObjectType), "guildonly")
    val META_HOMEGUILDONLY = CommandMeta.Key.of(TypeToken.get(Boolean::class.javaObjectType), "homeguildonly")
    val META_OWNERSONLY = CommandMeta.Key.of(TypeToken.get(Boolean::class.javaObjectType), "ownersonly")
    val META_COOLDOWN = CommandMeta.Key.of(TypeToken.get(CooldownMeta::class.java), "cooldown")
    val META_ADMINCHANNELONLY = CommandMeta.Key.of(TypeToken.get(Boolean::class.javaObjectType), "adminchannelonly")

    /**
     * Registers two builder modifiers:
     * - one which adds `META_AUTOPERM_ALLOWED` to commands that have the `NoAutoPermission` annotation
     * - another which applies the automatic permission to commands which are permitted to use automatic permissions
     *
     * @param C Command sender type
     * @param parser The annotation parser to register the builder modifiers with
     */
    fun <C> registerAutoPermission(parser: AnnotationParser<C>) {
        parser.registerBuilderModifier(NoAutoPermission::class.java) { _: NoAutoPermission, builder: Command.Builder<C> ->
            builder.meta(META_AUTOPERM_ALLOWED, false)
        }

        parser.registerBuilderModifier(CommandMethod::class.java) { _: CommandMethod, builder: Command.Builder<C> ->
            val command = builder.build()

            if (!command.commandMeta.get(META_AUTOPERM_ALLOWED).orElseGet { true }) {
                return@registerBuilderModifier builder
            }

            val components = command.components.filter { it.argument is StaticArgument }
            val permissionString = components.joinToString(separator = ".") {
                it.argument.name
            }
            var permission: CommandPermission = Permission.of(permissionString)

            // don't replace permission
            if (builder.commandPermission() != Permission.empty()) {
                permission = OrPermission.of(
                        listOf(
                                builder.commandPermission(),
                                permission
                        ))
            }

            logger.debug("Applied automatic permission $permission")
            builder.permission(permission)
        }
    }

    /**
     * Registers a builder modifier for `CommandCategory` annotations that adds `META_CATEGORY` to the command meta containing
     * the command's specified category.
     *
     * @param C Command sender type
     * @param parser The annotation parser to register the builder modifier with
     */
    fun <C> registerCategories(parser: AnnotationParser<C>) {
        parser.registerBuilderModifier(CommandCategory::class.java) { annotation: CommandCategory, builder: Command.Builder<C> ->
            builder.meta(META_CATEGORY, annotation.category)
        }
    }

    /**
     * Registers a builder modifier for `Example` annotations that adds `META_EXAMPLE` to the command meta containing
     * all of the command's examples.
     *
     * @param C Command sender type
     * @param parser The annotation parser to register the builder modifier with
     */
    fun <C> registerExamples(parser: AnnotationParser<C>) {
        parser.registerBuilderModifier(Example::class.java) { annotation: Example, builder: Command.Builder<C> ->
            builder.meta(META_EXAMPLE, annotation.examples)
        }
    }

    /**
     * Registers a builder modifier for `DiscordPermission` annotations that converts and adds Discord permissions
     * to command permissions.
     *
     * @param C Command sender type
     * @param parser The annotation parser to register the builder modifier with
     */
    fun <C> registerDiscordPermissions(parser: AnnotationParser<C>) {
        parser.registerBuilderModifier(DiscordPermission::class.java) { annotation: DiscordPermission, builder: Command.Builder<C> ->
            val permissions = annotation.permissions.map {
                Permission.of(
                        PermissionRegistry.convertPermission(it)
                )
            }
            var permission = AndPermission.of(permissions)
            val existingPermission = builder.commandPermission()

            if (existingPermission != Permission.empty()) {
                val newPermissions = listOf(existingPermission, permission)
                permission = when (annotation.mergeType) {
                    DiscordPermission.PermissionMerge.AND -> AndPermission.of(newPermissions)
                    DiscordPermission.PermissionMerge.OR -> OrPermission.of(newPermissions)
                    DiscordPermission.PermissionMerge.OVERRIDE -> permission
                }
            }

            builder.permission(permission)
        }
    }

    /**
     * Registers a:
     * - builder modifier that adds `META_COOLDOWN` to the command meta with the command's cooldown specifications
     * - command post processor that checks if the command is on cooldown
     *
     * From the command postprocessor, if the command is on cooldown then the command pipeline will be interrupted
     * and the [notifier] parameter will be called with the milliseconds remaining on the cooldown.
     *
     * @param C Command sender type
     * @param parser The annotation parser to register the builder modifier with
     * @param cmdManager The command manager to register the command post processor with
     * @param cooldownStorage Where cooldowns should be stored in
     * @param idMapper Mapper that converts a command sender to a [CooldownIds] object
     * @param notifier Notifier that gets called when a command is executed while it is on cooldown
     */
    fun <C> registerCooldown(parser: AnnotationParser<C>,
                             cmdManager: CommandManager<C>,
                             cooldownStorage: CooldownStorage,
                             idMapper: (C) -> CooldownIds,
                             notifier: (millisRemaining: Long, CooldownMeta, CommandPostprocessingContext<C>) -> Unit) {
        registerCommandLimit(
                parser,
                cmdManager,
                Cooldown::class.java,
                META_COOLDOWN,
                { cooldown: Cooldown, builder: Command.Builder<C> ->
                    builder.meta(META_COOLDOWN, CooldownMeta.fromAnnotation(cooldown))
                },
                { cooldownMeta: CooldownMeta, context: CommandPostprocessingContext<C> ->
                    val id = idMapper(context.commandContext.sender)
                    val time = cooldownStorage.applyCooldown(id, cooldownMeta, context.command)

                    time
                },
                notifier
        )
    }

    /**
     * Registers a:
     * - builder modifier that adds `META_GUILDONLY` to the command meta
     * - command post processor that checks if the command was executed in a guild
     *
     * From the command postprocessor, if the command was executed in a guild then the command pipeline will be interrupted
     * and the `notifier` parameter will be called.
     *
     * @param C Command sender type
     * @param parser The annotation parser to register the builder modifier with
     * @param cmdManager The command manager to register the command post processor with
     * @param notifier Notifier that gets called when a command is executed in a non-guild environment
     */
    fun <C> registerGuildOnly(parser: AnnotationParser<C>, cmdManager: CommandManager<C>, notifier: (CommandPostprocessingContext<C>) -> Unit) {
        registerBasicCommandLimit(
                parser,
                cmdManager,
                GuildOnly::class.java,
                META_GUILDONLY,
                limitation = { !it.commandContext.contains("Guild") },
                notifier = notifier
        )
    }

    /**
     * Registers a:
     * - builder modifier that adds `META_HOMEGUILDONLY` to the command meta
     * - command post processor that checks if the command was executed in the home guild
     *
     * From the command postprocessor, if the command was not executed in the home guild then the command pipeline will
     * be interrupted and the `notifier` parameter will be called.
     *
     * @param C Command sender type
     * @param parser The annotation parser to register the builder modifier with
     * @param cmdManager The command manager to register the command post processor with
     * @param notifier Notifier that gets called when a command is executed outside of the home guild
     */
    fun <C> registerHomeGuildOnly(parser: AnnotationParser<C>, cmdManager: CommandManager<C>, notifier: (CommandPostprocessingContext<C>) -> Unit) {
        registerBasicCommandLimit(
                parser,
                cmdManager,
                HomeGuildOnly::class.java,
                META_HOMEGUILDONLY,
                limitation = {
                    val guild = it.commandContext.get("Guild") as? Guild
                    val homeId = System.getenv()["HOME_GUILD_ID"]

                    // returns false if `homeId` is null
                    guild == null || homeId != null && guild.id != homeId
                },
                notifier = notifier
        )
    }

    /**
     * Registers a:
     * - builder modifier that adds `META_OWNERSONLY` to the command meta
     * - command post processor that checks if the command was executed by a bot (co)owner
     *
     * From the command postprocessor, if the command was not executed by a bot owner then the command pipeline will
     * be interrupted and the `notifier` parameter will be called.
     *
     * @param C Command sender type
     * @param parser The annotation parser to register the builder modifier with
     * @param cmdManager The command manager to register the command post processor with
     * @param notifier Notifier that gets called when a command is executed by someone who is not a bot owner
     * @param ownerIds The user IDs of the bot's owners
     */
    fun <C> registerOwnersOnly(
            parser: AnnotationParser<C>,
            cmdManager: CommandManager<C>,
            notifier: (CommandPostprocessingContext<C>) -> Unit,
            vararg ownerIds: String
    ) {
        registerBasicCommandLimit(
                parser,
                cmdManager,
                OwnersOnly::class.java,
                META_OWNERSONLY,
                limitation = {
                    val event = it.commandContext.get("MessageReceivedEvent") as? MessageReceivedEvent

                    event == null || !ownerIds.contains(event.author.id)
                },
                notifier = notifier
        )
    }

    /**
     * Registers a:
     * - builder modifier that adds `META_ADMINCHANNELONLY` to the command meta
     * - command post processor that checks if the command was executed in an admin channel
     *
     * From the command postprocessor, if the command was not executed in an admin channel then the command pipeline will
     * be interrupted and the `notifier` parameter will be called.
     *
     * Do note that this will block the command execution thread. You should be using an asynchronous command execution
     * coordinator, like [AsynchronousCommandExecutionCoordinator].
     *
     * @param C Command sender type
     * @param parser The annotation parser to register the builder modifier with
     * @param cmdManager The command manager to register the command post processor with
     * @param notifier Notifier that gets called when a command is executed outside of an admin channel
     */
    fun <C> registerRequireAdminChannel(parser: AnnotationParser<C>, cmdManager: CommandManager<C>, notifier: (CommandPostprocessingContext<C>) -> Unit) {
        registerBasicCommandLimit(
                parser,
                cmdManager,
                RequireAdminChannel::class.java,
                META_ADMINCHANNELONLY,
                limitation = {
                    // apply limitation if the channel wasn't found for any reason
                    val channel = it.commandContext.get("MessageChannel") as? MessageChannel
                            ?: return@registerBasicCommandLimit true

                    val isAdmin = ChannelDAO.instance.hasAttribute(channel.id, ChannelDTO.ChannelAttribute.ADMIN)
                            // assume it's not an admin channel if an error occurred
                            .onErrorReturn(false)
                            // it's safe to block since command execution will be on a different thread
                            .block()!!

                    !isAdmin
                },
                notifier = notifier
        )
    }

    /**
     * Generic annotation-based command execution limitation.
     *
     * This is a simpler version of [registerCommandLimit] that is only meant to limit based on a toggle.
     * If you need to apply more involved limitations (limiting based on a variety of factors or notifying with specific
     * data) you should look into using [registerCommandLimit] instead.
     *
     * Behind the scenes, this function registers a:
     * - builder modifier that adds the `key` parameter to the command meta with the value `true`
     * - command post processor that checks whether the `key` meta is on a command, and if so then whether the `limitation`
     * function parameter returns true.
     *
     * If the `limitation` function returns true at the time of calling, then the command execution will be aborted
     * and the `notifier` function will be called to notify the sender of the limitation.
     *
     * @param C Command sender type
     * @param A Annotation type
     * @param parser The annotation parser
     * @param cmdManager The command manager
     * @param annotationClass The annotation class linked to this limitation
     * @param key Command meta key used for marking a command with this limitation
     * @param builderModifier Modifies commands with the specified annotation (provided by `annotationClass`). By default,
     * this parameter applies the `key` to the command meta with the value `true`. This is done to mark the command
     * that this limitation should apply to it.
     * @param limitation Checks whether a command execution should be blocked
     * @param notifier Notifier that gets called when a command is blocked by this limitation
     */
    fun <C, A : Annotation> registerBasicCommandLimit(
            parser: AnnotationParser<C>,
            cmdManager: CommandManager<C>,
            annotationClass: Class<A>,
            key: CommandMeta.Key<Boolean>,
            builderModifier: (A, Command.Builder<C>) -> Command.Builder<C> =
                    { _: A, builder: Command.Builder<C> ->
                        builder.meta(key, true)
                    },
            limitation: (CommandPostprocessingContext<C>) -> Boolean,
            notifier: (CommandPostprocessingContext<C>) -> Unit
    ) {
        registerCommandLimit(
                parser,
                cmdManager,
                annotationClass,
                key,
                builderModifier,
                { _: Boolean, context: CommandPostprocessingContext<C> ->
                    if (limitation(context)) true else null
                },
                { _: Boolean, _: Boolean, context: CommandPostprocessingContext<C> ->
                    notifier(context)
                }
        )
    }

    /**
     * Generic annotation-based command execution limitation.
     *
     * Behind the scenes, this function registers a command post processor that checks whether the [key] meta is on a
     * command, and if so then whether the [limitation] function parameter returns a non-null value.
     *
     * If the [limitation] function returns a non-null value at the time of calling, then the command execution will be
     * aborted and the [notifier] function will be called with the result of [limitation] to notify the sender of the limit.
     *
     * @param C Command sender type
     * @param K Command meta type
     * @param D Limitation data type
     * @param A Annotation type
     * @param parser The annotation parser
     * @param cmdManager The command manager
     * @param annotationClass The annotation class linked to this limitation
     * @param key Command meta key used for marking a command with this limitation and possibly storing data about the limitation
     * @param builderModifier Modifies commands with the specified annotation (provided by `annotationClass`)
     * @param limitation Checks whether a command execution should be blocked and returns a non-null value if so.
     * The value returned will be passed to the [notifier] if it is not null
     * @param notifier Notifier that gets called when a command is blocked by this limitation
     */
    fun <C, K, D, A : Annotation> registerCommandLimit(
            parser: AnnotationParser<C>,
            cmdManager: CommandManager<C>,
            annotationClass: Class<A>,
            key: CommandMeta.Key<K>,
            builderModifier: (annotation: A, builder: Command.Builder<C>) -> Command.Builder<C>,
            limitation: (meta: K, context: CommandPostprocessingContext<C>) -> D?,
            notifier: (data: D, meta: K, context: CommandPostprocessingContext<C>) -> Unit
    ) {
        parser.registerBuilderModifier(annotationClass, builderModifier)

        cmdManager.registerCommandPostProcessor {
            val meta = it.command.commandMeta[key]
            if (meta.isPresent) {
                val data = limitation(meta.get(), it)

                if (data != null) {
                    notifier(data, meta.get(), it)
                    ConsumerService.interrupt()
                }
            }
        }
    }

    /**
     * Parse [DisplayName] annotations for arguments in class instance(s) and add the <argument hashcode = display name>
     * entries to a [CustomSyntaxFormatter].
     *
     * @param formatter The syntax formatter to store display name data
     * @param instances Instances of class(es) to parse
     */
    fun parseDisplayNames(formatter: CustomSyntaxFormatter<*>, instances: Array<Any>) {
        instances.flatMap { it.javaClass.declaredMethods.toList() }
                .filter { it.isAnnotationPresent(CommandMethod::class.java) }
                .flatMap { it.parameters.toList() }
                .filter {
                    it.isAnnotationPresent(Argument::class.java)
                            && it.isAnnotationPresent(DisplayName::class.java)
                }
                .forEach {
                    val argument = it.getAnnotation(Argument::class.java)
                    val displayName = it.getAnnotation(DisplayName::class.java)
                    val type = it.annotatedType
                    val argumentHash = Objects.hash(argument.value, it.type)
                    logger.debug("Hashed [${argument.value}] [${type.type.typeName}] -> $argumentHash")

                    formatter.displayNameMap[argumentHash] = displayName.name
                }
    }

    /**
     * Parses an array of classes for child commands (specified by [ChildCommands] annotations) and creates an instance
     * of each.
     *
     * The child commands should not have a constructor which requires parameters.
     *
     * @param parents Array of parent commands
     * @return The initialised child commands
     */
    fun parseChildCommands(parents: Array<Any>): List<Any> {
        return parents.filter { it.javaClass.isAnnotationPresent(ChildCommands::class.java) }
                .flatMap { it.javaClass.getAnnotation(ChildCommands::class.java).childCommands.toList() }
                .map { it.createInstance() }
    }
}