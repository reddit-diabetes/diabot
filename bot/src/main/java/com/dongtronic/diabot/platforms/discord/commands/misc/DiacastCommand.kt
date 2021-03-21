package com.dongtronic.diabot.platforms.discord.commands.misc

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.exceptions.NoSuchEpisodeException
import com.dongtronic.diabot.logic.`fun`.Diacast
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.rometools.rome.feed.synd.SyndEntry
import net.dv8tion.jda.api.EmbedBuilder

class DiacastCommand {
    @Example(["[diacast]", "[diacast] 6"])
    @CommandMethod("diacast [episode]")
    @CommandDescription("Get information about a diacast episode")
    @CommandCategory(Category.FUN)
    fun execute(
            sender: JDACommandUser,
            @Argument("episode", description = "The episode number to grab information about", defaultValue = "0")
            episodeNumber: Int
    ) {
        try {
            val episode = Diacast.getEpisode(episodeNumber)

            val builder = EmbedBuilder()

            buildEpisodeCard(episode, builder)

            sender.reply(builder.build()).subscribe()
        } catch (ex: NoSuchEpisodeException) {
            sender.replyErrorS("Episode $episodeNumber does not exist")
        } catch (ex: Exception) {
            sender.replyErrorS("Something went wrong: " + ex.message)
        }

    }

    private fun buildEpisodeCard(episode: SyndEntry, builder: EmbedBuilder) {
        builder.setTitle(episode.title, episode.link)
        builder.setAuthor("Diacast")

        for (element in episode.foreignMarkup) {
            if (element.name == "summary") {
                builder.setDescription(element.value)
            }

            if (element.name == "image") {
                val imageUrl = element.getAttributeValue("href")
                builder.setThumbnail(imageUrl)
            }

        }
    }
}
