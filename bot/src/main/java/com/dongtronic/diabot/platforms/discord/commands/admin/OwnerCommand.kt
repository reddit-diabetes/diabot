package com.dongtronic.diabot.platforms.discord.commands.admin

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.Hidden
import com.dongtronic.diabot.Main
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser

class OwnerCommand {
    @Hidden
    @CommandMethod("hi|hello|sup|owner")
    @CommandDescription("Say hi")
    @CommandCategory(Category.FUN)
    fun execute(sender: JDACommandUser) {
        val nickname = sender.getAuthorDisplayName()
        if (Main.ownerIds.contains(sender.getAuthorUniqueId())) {
            sender.replyS(":wave: Hello $nickname :)\nThanks for making me :heart:")
        } else {
            sender.replyS(":wave: Hello $nickname")
        }
    }

}