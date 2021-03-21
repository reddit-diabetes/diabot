package com.dongtronic.diabot.platforms.discord.commands.admin

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.Hidden
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.NoAutoPermission
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger

/**
 * @author John Grosh (jagrosh)
 */
class ShutdownCommand {
    private val logger = logger()

    @Hidden
    @NoAutoPermission
    @CommandMethod("shutdown|heckoff|fuckoff|removethyself|remove")
    @CommandDescription("Safely shuts off the bot")
    @CommandCategory(Category.ADMIN)
    fun execute(sender: JDACommandUser) {
        val allowedUsers = System.getenv()["superusers"]?.split(",") ?: emptyList()
        val allowed = allowedUsers.contains(sender.getAuthorUniqueId())

        if (allowed) {
            logger.info("Shutting down bot (requested by ${sender.getAuthorName()} (${sender.getAuthorUniqueId()}))")
            sender.replyWarningS("Shutting down (requested by ${sender.getAuthorDisplayName()})")
            sender.reactWarningS()
            sender.event.jda.shutdown()
        }
    }
}