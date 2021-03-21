package com.dongtronic.diabot.platforms.discord.commands.misc

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.Hidden
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.GuildOnly
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import java.time.temporal.ChronoUnit

class PingCommand {
    @Hidden
    @GuildOnly
    @CommandMethod("ping|pong")
    @CommandDescription("Checks the bot's latency")
    @CommandCategory(Category.UTILITIES)
    fun execute(sender: JDACommandUser) {
        sender.reply("Ping: ...").subscribe { m ->
            val ping = sender.event.message.timeCreated.until(m.timeCreated, ChronoUnit.MILLIS)
            m.editMessage("Ping: " + ping + "ms | Websocket: " + sender.event.jda.gatewayPing + "ms").queue()
        }
    }
}