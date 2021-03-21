package com.dongtronic.diabot.platforms.discord.commands.nightscout

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.NoAutoPermission
import com.dongtronic.diabot.data.mongodb.NightscoutDAO
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutDisplayCommands.DisplayOptions.Companion.sortOptions
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import java.util.*

class NightscoutDisplayCommands {
    private val logger = logger()

    @NoAutoPermission
    @CommandMethod("nightscout display list")
    @CommandDescription("Shows the display options available and the ones you have activated")
    @CommandCategory(Category.BG)
    fun listOptions(e: JDACommandUser) {
        val builder = MessageBuilder()
        val embed = EmbedBuilder()
        embed.setTitle("Valid Display Options", "https://github.com/reddit-diabetes/diabot/blob/master/docs/commands/Nightscout.md")
        embed.setColor(0x007fa7)

        for (option in DisplayOptions.values()) {
            if (option.description == null) continue

            val name = option.name.toLowerCase()
            embed.addField("__`$name`__", option.description, false)
        }

        builder.setEmbed(embed.build())

        NightscoutDAO.instance.getUser(e.getAuthorUniqueId()).subscribe({ dto ->
            val usersOptions = dto.displayOptions.sortOptions().joinToString { it.name.toLowerCase() }

            builder.append("Your current display preferences are:").appendCodeBlock(usersOptions, "")
            e.replyS(builder.build())
        }, {
            if (it !is NoSuchElementException) {
                logger.warn("Could not retrieve Nightscout user", it)
                builder.append("Your display preferences were not able to be retrieved.")
                e.replyS(builder.build())
            } else {
                builder.append("You currently do not have any display preferences set.")
                e.replyS(builder.build())
            }
        })
    }

    @NoAutoPermission
    @CommandMethod("nightscout display set <options>")
    @CommandDescription("Sets your display preferences")
    @CommandCategory(Category.BG)
    fun setOptions(e: JDACommandUser, @Argument("options") options: Array<String>) {
        updateOptions(e, options, NightscoutDAO.UpdateMode.SET)
    }

    @NoAutoPermission
    @CommandMethod("nightscout display add|a <options>")
    @CommandDescription("Adds display options to your preferences")
    @CommandCategory(Category.BG)
    fun addOptions(e: JDACommandUser, @Argument("options") options: Array<String>) {
        updateOptions(e, options, NightscoutDAO.UpdateMode.ADD)
    }

    @NoAutoPermission
    @CommandMethod("nightscout display remove|rem|del|d <options>")
    @CommandDescription("Removes display options from your preferences")
    @CommandCategory(Category.BG)
    fun removeOptions(e: JDACommandUser, @Argument("options") options: Array<String>) {
        updateOptions(e, options, NightscoutDAO.UpdateMode.DELETE)
    }

    private fun updateOptions(e: JDACommandUser, options: Array<String>, updateMode: NightscoutDAO.UpdateMode) {
        val errors = mutableListOf<String>()
        var newOptions = options.mapNotNull {
            try {
                DisplayOptions.valueOf(it.toUpperCase())
            } catch (exc: IllegalArgumentException) {
                errors.add(it.toUpperCase())
                null
            }
        }

        newOptions.firstOrNull { it.special }?.let {
            if (it == DisplayOptions.ALL) {
                newOptions = DisplayOptions.validOptions
            } else if (it == DisplayOptions.NONE) {
                newOptions = emptyList()
            }
        }

        NightscoutDAO.instance.updateDisplay(
                e.getAuthorUniqueId(),
                updateMode,
                newOptions.toSet()
        ).subscribe({ updatedOptions ->
            val formattedOptions = formatOptions(updatedOptions) { it.name.toLowerCase() }
            val message = "Nightscout display options were updated to $formattedOptions"

            if (errors.isEmpty()) {
                e.replySuccessS(message)
            } else {
                val formattedErrors = formatOptions(errors) { it.toLowerCase() }

                e.replyWarningS("$message. The following options were ignored because they are not valid options: $formattedErrors")
            }
        }, {
            logger.warn("Could not change Nightscout display options", it)
            e.replyErrorS("An error occurred while updating Nightscout display options")
        })
    }

    @NoAutoPermission
    @CommandMethod("nightscout display reset")
    @CommandDescription("Resets your display preferences to default")
    @CommandCategory(Category.BG)
    fun resetOptions(e: JDACommandUser) {
        NightscoutDAO.instance.updateDisplay(
                e.getAuthorUniqueId(),
                NightscoutDAO.UpdateMode.SET,
                null
        ).subscribe({
            e.replySuccessS("Nightscout display options were reset")
        }, {
            logger.warn("Could not reset Nightscout display options", it)
            e.replyErrorS("An error occurred while resetting Nightscout display options")
        })
    }

    private fun <T> formatOptions(options: Collection<T>, transform: ((T) -> CharSequence)? = null): String {
        return options.joinToString(
                prefix = "`",
                separator = "`, `",
                postfix = "`",
                transform = transform
        ).replace("``", "`<none>`")
    }

    enum class DisplayOptions(val description: String? = null, val special: Boolean = false) {
        ALL(special = true),
        NONE(special = true),

        TITLE("Your Nightscout display title."),
        TREND("The current BG trend arrow in Nightscout."),
        COB("The amount of carbs on board."),
        IOB("The amount of insulin on board."),
        AVATAR("Display your Discord profile picture in the embed."),
        // this seems pointless. remove in the future? todo
        SIMPLE("Hide your Discord profile picture from the embed.");

        companion object {
            /**
             * The default options to use when a user does not define their settings manually.
             */
            val defaults = setOf(TITLE, TREND, COB, IOB, AVATAR)

            /**
             * All valid options that are not special.
             */
            val validOptions = values().filter { !it.special }

            /**
             * All of the valid options in a displayable format (lowercase).
             */
            val optionsForDisplay = values().filter { !it.special }.optionsForDisplay()

            /**
             * Convert display options into a human-readable format.
             *
             * @receiver Collection of [DisplayOptions] objects
             * @return A list of the options in a displayable format (lowercase)
             */
            fun <T : Collection<DisplayOptions>> T.optionsForDisplay(): List<String> {
                return map { it.name.toLowerCase() }
            }

            /**
             * Sort the collection of [DisplayOptions] objects based on their appearance in the enum.
             *
             * @receiver Collection of [DisplayOptions] objects
             * @return A sorted set of [DisplayOptions] objects
             */
            fun <T : Collection<DisplayOptions>> T.sortOptions(): SortedSet<DisplayOptions> {
                val order = values().withIndex().associate { it.value to it.index }

                return sortedBy { order[it] }.toSortedSet()
            }
        }
    }
}