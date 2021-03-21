package com.dongtronic.diabot.platforms.discord.commands.nightscout

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DiscordPermission
import com.dongtronic.diabot.commands.annotations.GuildOnly
import com.dongtronic.diabot.commands.annotations.RequireAdminChannel
import com.dongtronic.diabot.data.mongodb.ChannelDAO
import com.dongtronic.diabot.data.mongodb.ChannelDTO
import com.dongtronic.diabot.mapNotNull
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageChannel

class NightscoutAdminSimpleCommands {
    private val logger = logger()

    @RequireAdminChannel
    @GuildOnly
    @DiscordPermission(Permission.MANAGE_CHANNEL)
    @CommandMethod("nightscoutadmin|nsadmin|na simple|shortchannels|simplechannels|sc list|l")
    @CommandDescription("List all channels where diabot will use simple nightscout replies")
    @CommandCategory(Category.ADMIN)
    fun listChannels(sender: JDACommandUser) {
        val event = sender.event
        ChannelDAO.instance.getChannels(event.guild.id)
                .filter { it.attributes.contains(ChannelDTO.ChannelAttribute.NIGHTSCOUT_SHORT) }
                .mapNotNull { event.guild.getTextChannelById(it.channelId) }
                .collectList()
                .subscribe({ channels ->
                    val builder = EmbedBuilder()

                    builder.setTitle("Short Nightscout channels")

                    if (channels.isEmpty()) {
                        builder.setDescription("No short channels are configured")
                    } else {
                        channels.forEach {
                            builder.appendDescription("**${it.name}**  (`${it.id}`)\n")
                        }
                    }

                    sender.reply(builder.build()).subscribe()
                }, {
                    val msg = "Could not access list of short Nightscout channels"
                    logger.warn(msg + " for ${event.guild.id}", it)
                    sender.replyErrorS(msg)
                })
    }

    @RequireAdminChannel
    @GuildOnly
    @DiscordPermission(Permission.MANAGE_CHANNEL)
    @CommandMethod("nightscoutadmin simple add|a <channel>")
    @CommandDescription("Add a simple response channel")
    @CommandCategory(Category.ADMIN)
    fun addChannel(
            sender: JDACommandUser,
            @Argument("channel", description = "The channel to add as a simple response channel")
            channel: MessageChannel
    ) {
        val event = sender.event
        logger.info("Adding channel ${channel.id} as short channel for ${event.guild.id}")

        ChannelDAO.instance.changeAttribute(event.guild.id, channel.id, ChannelDTO.ChannelAttribute.NIGHTSCOUT_SHORT)
                .subscribe({
                    sender.replySuccessS("Set channel **${channel.name}** (`${channel.id}`) as short reply channel")
                }, {
                    val msg = "Could not set channel ${channel.name} (${channel.id}) as short reply channel"
                    logger.warn(msg, it)
                    sender.replyErrorS(msg)
                })
    }

    @RequireAdminChannel
    @GuildOnly
    @DiscordPermission(Permission.MANAGE_CHANNEL)
    @CommandMethod("nightscoutadmin simple delete|del|d|remove|rem|r <channel>")
    @CommandDescription("Delete a simple response channel")
    @CommandCategory(Category.ADMIN)
    fun removeChannel(
            sender: JDACommandUser,
            @Argument("channel", description = "The channel to remove as a simple response channel")
            channel: MessageChannel
    ) {
        val event = sender.event
        logger.info("Removing channel ${channel.id} as short channel for ${event.guild.id}")

        ChannelDAO.instance.changeAttribute(event.guild.id, channel.id, ChannelDTO.ChannelAttribute.NIGHTSCOUT_SHORT, false)
                .subscribe({
                    sender.replySuccessS("Removed channel **${channel.name}** (`${channel.id}`) as short reply channel")
                }, {
                    val msg = "Could not remove channel ${channel.name} (${channel.id}) as short reply channel"
                    logger.warn(msg, it)
                    sender.replyErrorS(msg)
                })
    }
}