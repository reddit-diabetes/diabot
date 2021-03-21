package com.dongtronic.diabot.platforms.discord.commands.nightscout

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DisplayName
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.data.mongodb.NightscoutDAO
import com.dongtronic.diabot.graph.PlottingStyle
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger

class NightscoutGraphModeCommand {
    private val logger = logger()

    @CommandMethod("nightscoutgraph settings|setting|s mode|plot|m [mode]")
    @CommandDescription("Switches the plotting style of the graph between a scatter plot and a line plot")
    @CommandCategory(Category.BG)
    @Example(["[mode]", "[mode] scatter", "[mode] line"])
    fun execute(
            sender: JDACommandUser,
            @Argument("mode", description = "The plotting style to switch to")
            @DisplayName("scatter/line")
            @Greedy
            args: Array<String>?
    ) {
        val mode = NightscoutDAO.instance.getUser(sender.getAuthorUniqueId())
                .map { it.graphSettings }
                .map { settings ->
                    val newStyle = if (args.isNullOrEmpty()) {
                        PlottingStyle.values().first { it != settings.plotMode }
                    } else {
                        PlottingStyle.values().first { it.name.startsWith(args[0], true) }
                    }

                    settings.copy(plotMode = newStyle)
                }

        val command = mode.flatMap {
            NightscoutDAO.instance.updateGraphSettings(sender.getAuthorUniqueId(), it)
        }

        command.subscribe({
            sender.replySuccessS("Plotting style changed to `${it.plotMode.name}`")
        }, {
            if (it is NoSuchElementException && !args.isNullOrEmpty()) {
                sender.replyErrorS("No plotting mode known by the name of `${args[0]}`")
            } else {
                sender.replyErrorS("Could not update plotting style")
                logger.warn("Unexpected error when changing graph mode for ${sender.event.author}", it)
            }
        })
    }
}