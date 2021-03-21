package com.dongtronic.diabot.platforms.discord.commands.admin

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DiscordPermission
import com.dongtronic.diabot.commands.annotations.GuildOnly
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel

class AdminAnnounceCommand {
    private val logger = logger()

    @GuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("admin|a announce <channel> <message>")
    @CommandDescription("Announce a message in a channel")
    @CommandCategory(Category.ADMIN)
    fun execute(
            sender: JDACommandUser,
            @Argument("channel", description = "The channel to announce in")
            channel: MessageChannel,
            @Greedy
            @Argument("message", description = "The message to send in the channel")
            message: String
    ) {
        val mention = (if (channel is TextChannel) channel.asMention else channel.name) + " (`${channel.id}`)"

        kotlin.runCatching {
            channel.sendMessage(message).queue()
        }.onSuccess {
            sender.replySuccessS("Sent announcement to $mention")
        }.onFailure {
            logger.info("Could not send announcement `$message` to channel $mention", it)
            sender.replyErrorS("Could not send announcement to $mention: ${it.message}")
        }
    }
}
