package com.dongtronic.diabot.commands.cooldown

import cloud.commandframework.Command
import reactor.core.Disposable
import reactor.core.scheduler.Schedulers
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * Storage for cooldowns.
 *
 * @param cleanerSettings The `initialDelay`, `period`, and `unit` values to provide when creating a cleaner.
 *
 * These parameters are identical to [reactor.core.scheduler.Scheduler.schedulePeriodically]'s last three parameters.
 *
 * Specify this as `null` if no cleaner should be created.
 */
class CooldownStorage(cleanerSettings: Triple<Long, Long, TimeUnit>? = null) : Closeable {
    private val coolingEntities: MutableMap<String, Long> = mutableMapOf()
    private val cleanerDisposable: Disposable?

    init {
        cleanerDisposable = if (cleanerSettings != null) {
            scheduleCleaner(
                    cleanerSettings.first,
                    cleanerSettings.second,
                    cleanerSettings.third
            )
        } else {
            null
        }
    }

    /**
     * Apply a cooldown to a specific command for a scope.
     *
     * @param ids A [CooldownIds] instance containing the identifiers for the scope that the cooldown should be
     * applied upon
     * @param meta Cooldown metadata
     * @param command Command that should be cooled down before further usage by the scope
     * @return `null` if the cooldown was applied successfully.
     *
     * If the command was already cooling down for the given scope, the number of milliseconds remaining in the
     * cooldown will be returned instead.
     */
    fun applyCooldown(ids: CooldownIds, meta: CooldownMeta, command: Command<*>): Long? {
        val key = meta.scope.generateKey(command, ids)
        val currentTime = System.currentTimeMillis()

        if (coolingEntities.containsKey(key)
                && currentTime <= coolingEntities.getValue(key)) {
            // time remaining
            return coolingEntities.getValue(key) - currentTime
        }


        coolingEntities[key] = currentTime + meta.millis
        return null
    }

    /**
     * Remove a cooldown of a specific command for a scope.
     *
     * @param ids A [CooldownIds] instance containing the identifiers for the scope that should have its cooldown removed
     * @param meta Cooldown metadata
     * @param command Command that should have its cooldown removed for a scope
     * @return If the cooldown was removed successfully
     */
    fun removeCooldown(ids: CooldownIds, meta: CooldownMeta, command: Command<*>): Boolean {
        val key = meta.scope.generateKey(command, ids)
        return coolingEntities.remove(key) != null
    }

    private fun scheduleCleaner(initialDelay: Long, period: Long, unit: TimeUnit): Disposable {
        return Schedulers.boundedElastic().schedulePeriodically({
            val currentTime = System.currentTimeMillis()
            // clean entries

            coolingEntities.filterValues {
                it <= currentTime
            }.forEach {
                coolingEntities.remove(it.key, it.value)
            }
        }, initialDelay, period, unit)
    }

    override fun close() {
        coolingEntities.clear()
        cleanerDisposable?.dispose()
    }
}