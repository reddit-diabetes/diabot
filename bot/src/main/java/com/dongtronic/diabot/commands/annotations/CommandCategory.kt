package com.dongtronic.diabot.commands.annotations

import com.dongtronic.diabot.commands.Category

/**
 * Sets the category that a command will appear in.
 *
 * @property category The [Category] to set for the command
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CommandCategory(val category: Category)
