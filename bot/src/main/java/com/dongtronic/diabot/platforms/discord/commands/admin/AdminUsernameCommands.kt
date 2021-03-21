package com.dongtronic.diabot.platforms.discord.commands.admin

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DiscordPermission
import com.dongtronic.diabot.commands.annotations.GuildOnly
import com.dongtronic.diabot.data.mongodb.NameRuleDAO
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.Permission

class AdminUsernameCommands {
    private val logger = logger()

    @GuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("admin usernames|username|u enable|e")
    @CommandDescription("Enable username pattern enforcement")
    @CommandCategory(Category.ADMIN)
    fun enableEnforcement(sender: JDACommandUser) {
        val guild = sender.event.guild

        NameRuleDAO.instance.setEnforcing(guild.id, true).subscribe({
            sender.replySuccessS("Enabled username enforcement for ${guild.name}")
        }, {
            logger.warn("Could not enable username enforcement for guild ${guild.id}", it)
            sender.replyErrorS("Could not enable username enforcement for ${guild.name}")
        })
    }

    @GuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("admin usernames disable|d")
    @CommandDescription("Disable username pattern enforcement")
    @CommandCategory(Category.ADMIN)
    fun disableEnforcement(sender: JDACommandUser) {
        val guild = sender.event.guild

        NameRuleDAO.instance.setEnforcing(guild.id, false).subscribe({
            sender.replySuccessS("Disabled username enforcement for ${guild.name}")
        }, {
            logger.warn("Could not disable username enforcement for guild ${guild.id}", it)
            sender.replyErrorS("Could not disable username enforcement for ${guild.name}")
        })
    }

    @GuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("admin usernames hint|help|h [hint]")
    @CommandDescription("Set or view username enforcement hint")
    @CommandCategory(Category.ADMIN)
    fun hint(
            sender: JDACommandUser,
            @Greedy
            @Argument("hint", description = "The hint to give to a user whose username is violating the enforcement pattern")
            hint: String?
    ) {
        val guild = sender.event.guild
        if (hint == null) {
            NameRuleDAO.instance.getGuild(guild.id).subscribe({
                sender.replyS("Current username hint: `${it.hintMessage}`")
            }, {
                if (it is NoSuchElementException) {
                    sender.replyS("There is no username enforcement hint set")
                } else {
                    logger.warn("Could not get username enforcement hint for guild ${guild.id}", it)
                    sender.replyErrorS("Could not get username enforcement hint for ${guild.name}")
                }
            })
        } else {
            NameRuleDAO.instance.setHint(guild.id, hint).subscribe({
                sender.replySuccessS("Set username enforcement hint to `$hint`")
            }, {
                logger.warn("Could not set username enforcement hint for guild ${guild.id}", it)
                sender.replyErrorS("Could not set username enforcement hint for ${guild.name}")
            })
        }
    }

    @GuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("admin usernames pattern|pat|p|set|s [pattern]")
    @CommandDescription("Set or view username enforcement pattern")
    @CommandCategory(Category.ADMIN)
    fun pattern(
            sender: JDACommandUser,
            @Greedy
            @Argument("pattern", description = "The regex pattern to enforce for usernames")
            pattern: String?
    ) {
        val guild = sender.event.guild
        if (pattern == null) {
            NameRuleDAO.instance.getGuild(guild.id).subscribe({
                sender.replyS("Current username pattern: `${it.pattern}`")
            }, {
                if (it is NoSuchElementException) {
                    sender.replyS("There is no username enforcement pattern set")
                } else {
                    logger.warn("Could not get username enforcement pattern for guild ${guild.id}", it)
                    sender.replyErrorS("Could not get username enforcement pattern for ${guild.name}")
                }
            })
        } else {


            NameRuleDAO.instance.setPattern(guild.id, pattern).subscribe({
                sender.replySuccessS("Set username enforcement pattern to `$pattern`")
            }, {
                logger.warn("Could not set username enforcement pattern for guild ${guild.id}", it)
                sender.replyErrorS("Could not set username enforcement pattern for ${guild.name}")
            })
        }
    }
}