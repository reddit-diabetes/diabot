package com.dongtronic.diabot.commands.annotations

/**
 * Limits command execution to inside the home guild.
 *
 * This is set with the `HOME_GUILD_ID` environment variable.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class HomeGuildOnly
