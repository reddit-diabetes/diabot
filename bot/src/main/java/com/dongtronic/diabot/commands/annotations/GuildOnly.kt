package com.dongtronic.diabot.commands.annotations

/**
 * Specifies that a command can only be executed in a guild
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class GuildOnly
