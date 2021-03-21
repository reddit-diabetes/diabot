package com.dongtronic.diabot.commands.annotations

import com.dongtronic.diabot.commands.cooldown.CooldownScope
import java.util.concurrent.TimeUnit

/**
 * Sets a cooldown on command usage.
 *
 * @property time The amount of time that a cooldown should last
 * @property unit The unit that [time] should be interpreted as
 * @property scope The scope that the cooldown should be applied to
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Cooldown(
        val time: Long,
        val unit: TimeUnit,
        val scope: CooldownScope
)
