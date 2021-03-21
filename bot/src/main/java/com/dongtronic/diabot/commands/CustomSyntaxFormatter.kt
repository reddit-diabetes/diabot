package com.dongtronic.diabot.commands

import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.StandardCommandSyntaxFormatter
import java.util.*

/**
 * A custom syntax formatter which adds support for custom argument display names.
 *
 * @param C Command sender type
 */
class CustomSyntaxFormatter<C> : StandardCommandSyntaxFormatter<C>() {
    val displayNameMap = HashMap<Int, String>()

    override fun createInstance() = CustomFormattingInstance(displayNameMap)

    /**
     * @property displayNameMap Map which provides a custom argument display name (value) for the hashcode of an argument (key)
     */
    class CustomFormattingInstance(private val displayNameMap: HashMap<Int, String>) : FormattingInstance() {

        override fun appendRequired(argument: CommandArgument<*, *>) {
            appendName(this.requiredPrefix)
            appendName(getDisplayName(argument))
            appendName(this.requiredSuffix)
        }

        override fun appendOptional(argument: CommandArgument<*, *>) {
            appendName(this.optionalPrefix)
            appendName(getDisplayName(argument))
            appendName(this.optionalSuffix)
        }

        /**
         * Get the display name of a command argument
         *
         * @param arg CommandArgument to get display name of
         * @return Display name
         */
        private fun getDisplayName(arg: CommandArgument<*, *>) = displayNameMap.getOrDefault(hashArg(arg), arg.name)

        companion object {
            /**
             * Generate a custom hashcode for a [CommandArgument]
             *
             * @param arg The command argument to hash
             * @return hash code
             */
            fun hashArg(arg: CommandArgument<*, *>) = Objects.hash(arg.name, arg.valueType.type)
        }
    }
}