package com.dongtronic.diabot.platforms.discord.commands.admin

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.DiscordPermission
import com.dongtronic.diabot.commands.annotations.GuildOnly
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import net.dv8tion.jda.api.Permission

class RolesCommand {
    @GuildOnly
    @DiscordPermission(Permission.ADMINISTRATOR)
    @CommandMethod("roles")
    @CommandDescription("Get all roles in the server")
    @CommandCategory(Category.ADMIN)
    fun execute(sender: JDACommandUser) {
        val guild = sender.event.guild
        val roles = guild.roles

        val returned = StringBuilder()

        for (role in roles) {
            returned.append("\n").append(role.id).append(" - ").append(role.name)
        }

        sender.replyS(returned.toString())
    }
}
