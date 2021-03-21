package com.dongtronic.diabot.commands.cooldown

/**
 * Data object containing unique identifiers (snowflakes in the context of Discord) that are used to identify a scope
 * that is cooling down.
 *
 * @property userId The command sender's unique ID
 * @property channelId The unique ID for the channel that the command was executed in
 * @property guildId The unique ID for the guild that the command was executed in
 * @property shardId The unique ID for the shard that the command was executed in
 */
data class CooldownIds(
        val userId: String,
        val channelId: String,
        val guildId: String,
        val shardId: String
)
