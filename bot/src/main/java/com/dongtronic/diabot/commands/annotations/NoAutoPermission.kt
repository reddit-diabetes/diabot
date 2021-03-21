package com.dongtronic.diabot.commands.annotations

/**
 * Disables automatic permission generation for a command
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class NoAutoPermission
