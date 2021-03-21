package com.dongtronic.diabot.commands.cooldown

import com.dongtronic.diabot.commands.annotations.Cooldown

/**
 * Wrapper for [Cooldown] annotations providing the metadata for a cooldown.
 *
 * @property millis The amount of milliseconds this cooldown should last
 * @property scope The scope that the cooldown should be applied to
 */
data class CooldownMeta(val millis: Long, val scope: CooldownScope) {
    companion object {
        /**
         * Create a [CooldownMeta] instance from a [Cooldown] annotation.
         *
         * @param cooldown The annotation to create from
         * @return A [CooldownMeta] instance with the data stored in a [Cooldown] annotation.
         */
        fun fromAnnotation(cooldown: Cooldown): CooldownMeta {
            return CooldownMeta(cooldown.unit.toMillis(cooldown.time), cooldown.scope)
        }
    }
}