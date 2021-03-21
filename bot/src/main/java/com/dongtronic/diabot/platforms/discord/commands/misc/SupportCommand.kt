package com.dongtronic.diabot.platforms.discord.commands.misc

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import net.dv8tion.jda.api.EmbedBuilder

class SupportCommand {
    @CommandMethod("support")
    @CommandDescription("Get information about supporting Diabot development")
    @CommandCategory(Category.INFO)
    fun execute(sender: JDACommandUser) {
        val builder = EmbedBuilder()

        builder.setTitle("Diabot Support")

        builder.setDescription("Diabot costs money to run and time to develop. You can help!")

        builder.addField("Contribute Code", "https://github.com/reddit-diabetes/diabot", true)
        builder.addField("Donate money", "https://github.com/sponsors/cascer1", true)

        builder.setColor(java.awt.Color.orange)

        sender.reply(builder.build()).subscribe()
    }
}
