package com.dongtronic.diabot.platforms.discord.utils

import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object NicknameUtils {

    fun determineAuthorDisplayName(event: CommandEvent): String {
        return this.determineDisplayName(event.event, event.author)
    }

    fun determineDisplayName(event: CommandEvent, user: User): String {
        return this.determineDisplayName(event.event, user)
    }

    fun determineAuthorDisplayName(event: MessageReceivedEvent): String {
        return this.determineDisplayName(event, event.author)
    }

    fun determineDisplayName(event: MessageReceivedEvent, user: User): String {
        var name = user.name

        if (event.isFromGuild) {
            event.guild.getMember(user)?.run {
                name = this.effectiveName
            }
        }

        return name
    }
}
