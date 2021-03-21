package com.dongtronic.diabot.platforms.discord.commands.misc

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.Cooldown
import com.dongtronic.diabot.commands.cooldown.CooldownScope
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import java.util.concurrent.TimeUnit

class DisclaimerCommand {
    @CommandMethod("disclaimer")
    @CommandDescription("Show the disclaimer for diabot")
    @CommandCategory(Category.UTILITIES)
    @Cooldown(3, TimeUnit.MINUTES, CooldownScope.CHANNEL)
    fun execute(sender: JDACommandUser) {
        val text = this::class.java.classLoader.getResource("DISCLAIMER")?.readText()

        if (text != null) {
            sender.replyS(text)
        } else {
            sender.replyErrorS("Disclaimer could not be loaded.")
        }
    }
}
