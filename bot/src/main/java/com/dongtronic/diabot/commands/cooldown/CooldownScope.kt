package com.dongtronic.diabot.commands.cooldown

import cloud.commandframework.Command
import kotlin.reflect.KProperty1

/**
 * The scope that a cooldown should be applied to.
 *
 * @property keyProperties [CooldownIds] properties that the scope's key should use.
 */
enum class CooldownScope(vararg val keyProperties: KProperty1<CooldownIds, String>) {
    GLOBAL,
    CHANNEL(CooldownIds::channelId),
    GUILD(CooldownIds::guildId),
    SHARD(CooldownIds::shardId),

    USER(CooldownIds::userId),
    USER_CHANNEL(CooldownIds::userId, CooldownIds::channelId),
    USER_GUILD(CooldownIds::userId, CooldownIds::guildId),
    USER_SHARD(CooldownIds::userId, CooldownIds::shardId);

    /**
     * Generates a key to impose a cooldown upon a command within a specific scope.
     *
     * @param command The command to apply the cooldown upon
     * @param ids The [CooldownIds] instance to retrieve identifiers from
     * @return A key to identify a command within a specific scope.
     */
    fun generateKey(command: Command<*>, ids: CooldownIds): String {
        val keyBuilder = StringBuilder("${command}-")

        keyProperties.forEachIndexed { index, property ->
            if (index != 0) keyBuilder.append(";")

            keyBuilder.append(property.name.firstOrNull() ?: "noname")
            keyBuilder.append("=")
            keyBuilder.append(property.get(ids))
        }

        return keyBuilder.toString()
    }
}