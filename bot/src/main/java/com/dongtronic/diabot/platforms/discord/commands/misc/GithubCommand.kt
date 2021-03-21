package com.dongtronic.diabot.platforms.discord.commands.misc

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import net.dv8tion.jda.api.EmbedBuilder

class GithubCommand{
    @CommandMethod("github")
    @CommandDescription("Get the GitHub link for Diabot")
    @CommandCategory(Category.UTILITIES)
    fun execute(sender: JDACommandUser) {
        val builder = EmbedBuilder()

        builder.setTitle("Diabot GitHub")

        builder.setDescription("https://github.com/reddit-diabetes/diabot")

        builder.addField("Issues", "https://github.com/reddit-diabetes/diabot/issues", true)
        builder.addField("Bug report", "https://github.com/reddit-diabetes/diabot/issues/new?template=bug_report.md", true)
        builder.addField("Feature request", "https://github.com/reddit-diabetes/diabot/issues/new?template=feature_request.md", true)

        builder.setColor(java.awt.Color.blue)

        sender.reply(builder.build()).subscribe()
    }
}
