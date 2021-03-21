package com.dongtronic.diabot.commands.annotations

/**
 * Limits command execution to channels marked as admin only
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RequireAdminChannel
