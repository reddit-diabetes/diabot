package com.dongtronic.diabot.commands

import cloud.commandframework.CommandManager
import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.arguments.parser.StandardParameters
import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext
import cloud.commandframework.meta.CommandMeta
import com.dongtronic.diabot.commands.cooldown.CooldownIds
import com.dongtronic.diabot.commands.cooldown.CooldownMeta
import com.dongtronic.diabot.commands.cooldown.CooldownStorage

/**
 * Wrapper class for [AnnotationParser] which provides features unique to Diabot.
 *
 * @param C Command sender type
 * @property cmdManager The command manager
 * @property parser The annotation parser instance associated with this [DiabotParser] object
 * @property syntaxFormatter A custom syntax formatter for `DisplayName` annotations
 */
class DiabotParser<C>(
        private val cmdManager: CommandManager<C>,
        senderClass: Class<C>
) {
    val parser = createAnnotationParser(cmdManager, senderClass)
    val syntaxFormatter = CustomSyntaxFormatter<C>()

    init {
        cmdManager.commandSyntaxFormatter = syntaxFormatter
    }

    /**
     * Adds support for automatic permission setting to this parser.
     *
     * @return this [DiabotParser] instance
     */
    fun addAutoPermissionSupport() = apply { ParserUtils.registerAutoPermission(parser) }

    /**
     * Adds support for CommandCategory annotations to this parser.
     *
     * @return this [DiabotParser] instance
     */
    fun addCategorySupport() = apply { ParserUtils.registerCategories(parser) }

    /**
     * Adds support for Example annotations to this parser.
     *
     * @return this [DiabotParser] instance
     */
    fun addExampleSupport() = apply { ParserUtils.registerExamples(parser) }

    /**
     * Adds support for GuildOnly annotations to this parser.
     *
     * @param notifier Notifier that gets called when a command is executed in a non-guild environment
     * @return this [DiabotParser] instance
     */
    fun addGuildOnlySupport(notifier: (CommandPostprocessingContext<C>) -> Unit) = apply {
        ParserUtils.registerGuildOnly(parser, cmdManager, notifier)
    }

    /**
     * Adds support for HomeGuildOnly annotations to this parser.
     *
     * @param notifier Notifier that gets called when a command is executed outside of the home guild
     * @return this [DiabotParser] instance
     */
    fun addHomeGuildOnlySupport(notifier: (CommandPostprocessingContext<C>) -> Unit) = apply {
        ParserUtils.registerHomeGuildOnly(parser, cmdManager, notifier)
    }

    /**
     * Adds support for OwnersOnly annotations to this parser.
     *
     * @param ownerIds The user IDs of the bot's owners
     * @param notifier Notifier that gets called when a command is executed by someone who is not a bot owner
     * @return this [DiabotParser] instance
     */
    fun addOwnersOnlySupport(vararg ownerIds: String, notifier: (CommandPostprocessingContext<C>) -> Unit) = apply {
        ParserUtils.registerOwnersOnly(parser, cmdManager, notifier, *ownerIds)
    }

    /**
     * Adds support for RequireAdminChannel annotations to this parser.
     *
     * @param notifier Notifier that gets called when a command is executed outside of an admin channel
     * @return this [DiabotParser] instance
     */
    fun addAdminChannelOnlySupport(notifier: (CommandPostprocessingContext<C>) -> Unit) = apply {
        ParserUtils.registerRequireAdminChannel(parser, cmdManager, notifier)
    }

    /**
     * Adds support for Cooldown annotations to this parser.
     *
     * @param idMapper Mapper that converts a command sender to a [CooldownIds] object
     * @param notifier Notifier that gets called when a command is executed while it is on cooldown
     * @param cooldownStorage Where cooldowns should be stored in
     * @return this [DiabotParser] instance
     */
    fun addCooldownSupport(
            idMapper: (C) -> CooldownIds,
            notifier: (millisRemaining: Long, CooldownMeta, CommandPostprocessingContext<C>) -> Unit,
            cooldownStorage: CooldownStorage = CooldownStorage()
    ) = apply {
        ParserUtils.registerCooldown(parser, cmdManager, cooldownStorage, idMapper, notifier)
    }

    /**
     * Adds support for DiscordPermission annotations to this parser.
     *
     * @return this [DiabotParser] instance
     */
    fun addDiscordPermissionSupport() = apply { ParserUtils.registerDiscordPermissions(parser) }

    /**
     * Parse class instances for DisplayName annotations and (sub)commands.
     *
     * @param instances Array of classes to parse
     * @return this [DiabotParser] instance
     */
    fun parse(instances: Array<Any>) = apply {
        ParserUtils.parseDisplayNames(syntaxFormatter, instances)

        val childCommands = ParserUtils.parseChildCommands(instances)
        childCommands.plus(instances).forEach { parser.parse(it) }
    }

    companion object {
        /**
         * Creates an [AnnotationParser] instance with a command meta containing the command's description.
         *
         * @param cmdManager The command manager in use
         * @param senderClass The class of the command sender type
         * @return An annotation parser instance
         */
        fun <C> createAnnotationParser(
                cmdManager: CommandManager<C>,
                senderClass: Class<C>
        ) = AnnotationParser(cmdManager, senderClass) {
            CommandMeta.simple()
                    .with(CommandMeta.DESCRIPTION, it.get(StandardParameters.DESCRIPTION, "No description"))
                    .build()
        }
    }
}