package com.dongtronic.diabot.data.mongodb

import com.dongtronic.diabot.graph.GraphSettings
import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutDisplayCommands
import com.fasterxml.jackson.annotation.JsonIgnore
import net.dv8tion.jda.api.entities.User

data class NightscoutUserDTO(
        val userId: String = "",
        val url: String? = null,
        val token: String? = null,
        val displayOptions: Set<NightscoutDisplayCommands.DisplayOptions> = NightscoutDisplayCommands.DisplayOptions.defaults,
        val publicGuilds: List<String> = emptyList(),
        val graphSettings: GraphSettings = GraphSettings(),
        @JsonIgnore
        val jdaUser: User? = null
) {
    /**
     * Nightscout's API endpoint
     */
    val apiEndpoint: String
        @JsonIgnore
        get() {
            val trimmed = url!!.removeSuffix("/")
            return trimmed.plus("/api/v1/")
        }

    /**
     * Checks if this NS user has set their NS to be public in the provided guild.
     *
     * @param guildId The ID of the guild
     * @return Whether this Nightscout is public in this guild.
     */
    fun isNightscoutPublic(guildId: String): Boolean {
        return publicGuilds.contains(guildId) || userId.isBlank()
    }
}