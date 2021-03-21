package com.dongtronic.diabot.platforms.discord.commands.misc

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.ReplyType
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.logic.`fun`.ExcuseGetter
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import java.io.IOException

class ExcuseCommand {
    @CommandMethod("excuse")
    @CommandDescription("gibs excus")
    @CommandCategory(Category.FUN)
    fun execute(sender: JDACommandUser) {
        try {
            sender.replyS(ExcuseGetter.get(), ReplyType.NONE)
        } catch (e: IOException) {
            sender.replyErrorS("Oops")
        }
    }
}
