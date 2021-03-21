package com.dongtronic.diabot.platforms.discord.commands

import com.dongtronic.diabot.commands.ResponseLevel
import com.dongtronic.diabot.commands.ResponseLevelMapper

class ResponseEmojiMapper : ResponseLevelMapper {
    override fun getResponseIndicator(responseLevel: ResponseLevel, includeSpace: Boolean): String {
        val builder = StringBuilder()

        builder.append(when (responseLevel) {
            ResponseLevel.SUCCESS -> "\uD83D\uDC4C"
            ResponseLevel.NORMAL -> ""
            ResponseLevel.WARNING -> "\uD83D\uDE2E"
            ResponseLevel.ERROR -> "\uD83D\uDE22"
        })

        if (includeSpace) {
            builder.append(' ')
        }

        return builder.toString()
    }
}