package com.dongtronic.diabot.platforms.discord.commands.nightscout

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.*
import com.dongtronic.diabot.data.mongodb.NightscoutDAO
import com.dongtronic.diabot.data.mongodb.NightscoutUserDTO
import com.dongtronic.diabot.nameOf
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import com.mongodb.client.result.UpdateResult
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User

@ChildCommands(NightscoutAdminSimpleCommands::class)
class NightscoutAdminCommands {
    private val logger = logger()

    @OwnersOnly
    @HomeGuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("nightscoutadmin|nsadmin|na delete|del|d|remove|rm|r <user>")
    @CommandDescription("Delete a configured nightscout URL")
    @CommandCategory(Category.ADMIN)
    fun deleteUrl(
            sender: JDACommandUser,
            @Argument(value = "user",
                    description = "User to delete URL for",
                    parserName = "user-global")
            user: User
    ) {
        val event = sender.event
        val userId = user.id

        logger.info("Deleting Nightscout URL for user $userId [requested by ${event.author.name}]")

        NightscoutDAO.instance.deleteUser(userId, NightscoutUserDTO::url)
                .ofType(UpdateResult::class.java)
                .subscribe({
                    if (it.modifiedCount == 0L) {
                        sender.replyErrorS("User **${event.nameOf(user)}** (`$userId`) does not have a Nightscout URL configured")
                    } else {
                        sender.replySuccessS("Deleted Nightscout URL for user **${event.nameOf(user)}** (`$userId`)")
                    }
                }, {
                    val msg = "Could not delete Nightscout URL ${event.nameOf(user)} (`$userId`)"
                    logger.warn(msg, it)
                    sender.replyErrorS(msg)
                })
    }

    @OwnersOnly
    @HomeGuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("nightscoutadmin set|s <user> <url>")
    @CommandDescription("Set Nightscout URL for a user")
    @CommandCategory(Category.ADMIN)
    fun setUrl(
            sender: JDACommandUser,
            @Argument(value = "user",
                    description = "User to set URL for",
                    parserName = "user-global")
            user: User,
            @Argument(value = "url", description = "The URL to set")
            @Greedy
            urlInput: String
    ) {
        val event = sender.event
        val url = NightscoutSubcommands.validateNightscoutUrlOrNull(urlInput)

        if (url == null) {
            sender.replyErrorS("The URL provided is not a proper HTTP/HTTPS URL: `$urlInput`")
            return
        }

        logger.info("Admin setting URL for user ${event.nameOf(user)} to $url")

        NightscoutDAO.instance.setUrl(user.id, url).subscribe({
            sender.replySuccessS("Admin set Nightscout URL for ${event.nameOf(user)}")
        }, {
            val msg = "Could not set Nightscout URL for ${event.nameOf(user)} (${user.id})"
            logger.warn(msg, it)
            sender.replyErrorS(msg)
        })
    }


}