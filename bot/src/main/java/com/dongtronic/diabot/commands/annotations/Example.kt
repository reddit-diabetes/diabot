package com.dongtronic.diabot.commands.annotations

/**
 * Provides examples for command usage
 *
 * @property examples Command usage example(s)
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Example(val examples: Array<String>)