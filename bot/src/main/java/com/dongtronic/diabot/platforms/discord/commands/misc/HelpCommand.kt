package com.dongtronic.diabot.platforms.discord.commands.misc

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.DiabotHelp
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.NoAutoPermission

class HelpCommand<C>(private val diabotHelp: DiabotHelp<C>) {

    @NoAutoPermission
    @CommandMethod("help [query]")
    @CommandCategory(Category.INFO)
    fun help(
            sender: C,
            @Argument("query")
            @Greedy
            query: String?
    ) {
        diabotHelp.queryCommands(query ?: "", sender)
    }
}