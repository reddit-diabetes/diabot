package com.dongtronic.diabot.platforms.discord.commands.misc

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.exceptions.RequestStatusException
import com.dongtronic.diabot.logic.`fun`.Awyisser
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import net.dv8tion.jda.api.EmbedBuilder
import reactor.core.scheduler.Schedulers
import java.awt.Color

class AwyissCommand {
    @CommandMethod("awyiss|awwyiss|duck|breadcrumbs <phrase>")
    @CommandDescription("muther f'in breadcrumbs")
    @CommandCategory(Category.FUN)
    fun execute(
            sender: JDACommandUser,
            @Greedy
            @Argument("phrase", description = "Your phrase to pass into the comic")
            phrase: String
    ) {
        sender.reactSuccess()

        Awyisser.generate(phrase)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe({ imageUrl ->
                    val builder = EmbedBuilder()

                    builder.setTitle("Awyiss - $phrase")
                    builder.setAuthor(sender.getAuthorDisplayName())
                    builder.setImage(imageUrl)
                    builder.setColor(Color.white)

                    val embed = builder.build()

                    sender.reply(embed).subscribe()
                }, {
                    if (it is RequestStatusException) {
                        if (it.status == 500) {
                            sender.replyErrorS("Server error occurred.")
                            return@subscribe
                        }
                    }

                    sender.replyErrorS("Something went wrong: " + it.message)
                })
    }
}
