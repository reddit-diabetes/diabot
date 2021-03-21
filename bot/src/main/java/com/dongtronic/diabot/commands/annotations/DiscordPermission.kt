package com.dongtronic.diabot.commands.annotations

import net.dv8tion.jda.api.Permission

/**
 * Sets a command's permission to Discord-based permissions
 *
 * @property permissions The Discord [Permission]s that a command should require
 * @property mergeType How existing command permissions should be merged with Discord permissions
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class DiscordPermission(
        vararg val permissions: Permission,
        val mergeType: PermissionMerge = PermissionMerge.AND
) {
    /**
     * The permission-merging behaviour to follow if a command already has a permission associated with it.
     */
    enum class PermissionMerge {
        /**
         * Both the existing permissions and discord permissions must pass for the command executor.
         *
         * CommandPermission = ((discord permissions) AND (existing permissions))
         */
        AND,

        /**
         * Either the existing permissions or the discord permissions must pass for the command executor.
         *
         * CommandPermission = ((discord permissions) OR (existing permissions))
         */
        OR,

        /**
         * The existing permissions will be overwritten by the discord permissions and the old permissions will not apply
         * for the command.
         *
         * CommandPermission = (discord permissions)
         */
        OVERRIDE
    }
}
