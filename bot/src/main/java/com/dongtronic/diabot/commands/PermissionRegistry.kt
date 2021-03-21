package com.dongtronic.diabot.commands

import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import net.dv8tion.jda.api.Permission
import java.util.*

/**
 * todo
 */
class PermissionRegistry {
    companion object {
        /**
         * A map of Cloud discord permission names -> [Permission] enums.
         */
        val discordPermissions: Map<String, Permission> =
                Permission.values().associateBy { convertPermission(it) }

        /**
         * Convert a Discord [Permission] enum into a Cloud-compatible permission string.
         *
         * @param permission Discord permission enum
         * @return Cloud-compatible permission string
         */
        fun convertPermission(permission: Permission): String {
            return "discord." + permission.name
                    .toLowerCase(Locale.ENGLISH)
                    .replace('_', '-')
        }
    }

    /**
     * Check if a [JDACommandUser] has a permission.
     *
     * @param sender The user to verify has the permission
     * @param permission The permission string to check
     * @return If the sender
     */
    fun hasPermission(sender: JDACommandUser, permission: String): Boolean {
        val event = sender.event
        val discordPermission = discordPermissions[permission]

        if (discordPermission != null) {
            if (event.isFromGuild) {
                if (event.member != null) {
                    return event.member!!.hasPermission(event.textChannel, discordPermission)
                }
            } else {
                // i think it's safe to grant full discord permissions since it's a DM
                // not entirely sure though, TODO?
                return true
            }
        }

        return true
    }

}