package com.dongtronic.diabot.commands.annotations

/**
 * Customises an argument's name when displayed in syntax formatting.
 *
 * @property name New display name
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class DisplayName(val name: String)
