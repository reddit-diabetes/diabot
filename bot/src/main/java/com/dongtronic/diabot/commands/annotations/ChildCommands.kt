package com.dongtronic.diabot.commands.annotations

import kotlin.reflect.KClass

/**
 * Specifies additional classes which should be initialised and parsed for commands.
 *
 * @property childCommands The [KClass]es of the child commands
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ChildCommands(vararg val childCommands: KClass<*>)
