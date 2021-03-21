package com.dongtronic.diabot.commands.annotations

/**
 * Limits command execution to bot owners/co-owners only
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class OwnersOnly
