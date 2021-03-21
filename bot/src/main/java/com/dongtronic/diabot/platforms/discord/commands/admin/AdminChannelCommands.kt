package com.dongtronic.diabot.platforms.discord.commands.admin

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DiscordPermission
import com.dongtronic.diabot.commands.annotations.GuildOnly
import com.dongtronic.diabot.data.mongodb.ChannelDAO
import com.dongtronic.diabot.data.mongodb.ChannelDTO
import com.dongtronic.diabot.mapNotNull
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageChannel

class AdminChannelCommands {
    private val logger = logger()

    @GuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("admin channels|c add|a <channel>")
    @CommandDescription("Adds a channel as an admin channel")
    @CommandCategory(Category.ADMIN)
    fun addChannel(
            sender: JDACommandUser,
            @Argument("channel", description = "The channel to add as an admin channel")
            channel: MessageChannel
    ) {
        ChannelDAO.instance.changeAttribute(sender.event.guild.id, channel.id, ChannelDTO.ChannelAttribute.ADMIN)
                .subscribe({
                    sender.replySuccessS("Added admin channel ${channel.name} (`${channel.id}`)")
                }, {
                    val msg = "Could not set admin channel ${channel.name} (`${channel.id}`)"
                    logger.warn(msg, it)
                    sender.replyErrorS(msg)
                })
    }

    @GuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("admin channels delete|d|remove|r <channel>")
    @CommandDescription("Removes a channel as an admin channel")
    @CommandCategory(Category.ADMIN)
    fun deleteChannel(
            sender: JDACommandUser,
            @Argument("channel", description = "The channel to remove as an admin channel")
            channel: MessageChannel
    ) {
        ChannelDAO.instance.changeAttribute(sender.event.guild.id, channel.id, ChannelDTO.ChannelAttribute.ADMIN, false)
                .subscribe({
                    sender.replySuccessS("Removed admin channel ${channel.name} (`${channel.id}`)")
                }, {
                    val msg = "Could not remove admin channel ${channel.name} (`${channel.id}`)"
                    logger.warn(msg, it)
                    sender.replyErrorS(msg)
                })
    }

    @GuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("admin channels list|l")
    @CommandDescription("Lists admin channels")
    @CommandCategory(Category.ADMIN)
    fun listChannels(sender: JDACommandUser) {
        val guild = sender.event.guild

        ChannelDAO.instance.getChannels(guild.id)
                .filter { it.attributes.contains(ChannelDTO.ChannelAttribute.ADMIN) }
                .mapNotNull { guild.getTextChannelById(it.channelId) }
                .collectList()
                .subscribe({ channels ->
                    val builder = EmbedBuilder()

                    builder.setTitle("Admin channels")
                    if (channels.isEmpty()) {
                        builder.setDescription("No admin channels are configured")
                    } else {
                        channels.forEach {
                            builder.appendDescription("**${it.name}**  (`${it.id}`)\n")
                        }
                    }
                    sender.reply(builder.build()).subscribe()
                }, {
                    val msg = "Could not access list of admin channels"
                    logger.warn(msg + " for ${guild.id}", it)
                    sender.replyErrorS(msg)
                })
    }
}