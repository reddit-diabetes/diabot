package com.dongtronic.diabot.platforms.discord.commands.nightscout

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.ChildCommands
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.Cooldown
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.commands.cooldown.CooldownScope
import com.dongtronic.diabot.data.mongodb.NightscoutDAO
import com.dongtronic.diabot.graph.BgGraph
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import com.dongtronic.nightscout.Nightscout
import com.dongtronic.nightscout.data.NightscoutDTO
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.XYChart
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.TimeUnit


@ChildCommands(NightscoutGraphModeCommand::class)
class NightscoutGraphCommand {
    private val logger = logger()

    @Cooldown(5, TimeUnit.SECONDS, CooldownScope.USER)
    @Example(["[nightscoutgraph]"])
    @CommandMethod("nightscoutgraph|nsg|bgg|bsg|nsgraph|bggraph|bsgraph|graph")
    @CommandDescription("Generates a graph of your recent BG data from Nightscout")
    @CommandCategory(Category.BG)
    fun execute(sender: JDACommandUser) {
        val event = sender.event
        getDataSet(event.author.id)
                .map { BitmapEncoder.getBitmapBytes(it, BitmapEncoder.BitmapFormat.PNG) }
                .flatMap { event.channel.sendFile(it, "graph.png").submit().toMono() }
                .subscribe(null, {
                    sender.replyErrorS("Error generating graph")
                    logger.error("Error generating NS graph for ${event.author}", it)
                })
    }

    private fun getDataSet(sender: String): Mono<XYChart> {
        return NightscoutDAO.instance.getUser(sender)
                .zipWhen { userDTO ->
                    if (userDTO.url == null) {
                        return@zipWhen Mono.error<NightscoutDTO>(Exception("no url found"))
                    }

                    val ns = Nightscout(userDTO.url, userDTO.token)
                    // 12 readings per hour -> 4 hours of readings
                    ns.getRecentSgv(count = 12 * 4).flatMap { ns.getSettings(it) }
                }.map { tuple ->
                    val userDTO = tuple.t1
                    val ns = tuple.t2
                    BgGraph(userDTO.graphSettings).addEntries(ns)
                }
    }
}
