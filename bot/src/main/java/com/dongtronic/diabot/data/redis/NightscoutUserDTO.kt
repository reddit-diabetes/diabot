package com.dongtronic.diabot.data.redis

import com.dongtronic.diabot.platforms.discord.commands.nightscout.NightscoutDisplayCommands

class NightscoutUserDTO {
    var token: String? = null
    var displayOptions: Array<String> = NightscoutDisplayCommands.DisplayOptions.defaults
            .map { it.name.toLowerCase() }.toTypedArray()
    var avatarUrl: String? = null
        set (value) {
            if (value != null) {
                field = value
            }
        }
}