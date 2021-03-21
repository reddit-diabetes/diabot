package com.dongtronic.diabot.platforms.discord.listeners

import com.dongtronic.diabot.exceptions.UnknownUnitException
import com.dongtronic.diabot.logic.diabetes.BGConversionFormatter
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.Patterns
import com.dongtronic.diabot.util.logger
import com.github.ygimenez.method.Pages
import com.github.ygimenez.model.ThrowingBiConsumer
import com.github.ygimenez.type.Emote
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate

class ConversionListener(
        private val jdaCommandUpdateHandler: JDACommandUpdateHandler? = null
) : ListenerAdapter() {
    private val logger = logger()
    private val cancelButton = ThrowingBiConsumer { _: Member, message: Message ->
        message.delete().reason("unit conversion removed by author's request").queue()
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val message = event.message
        val messageText = message.contentRaw

        val inlineMatcher = Patterns.inlineBgPattern.matcher(messageText)
        val separateMatcher = Patterns.separateBgPattern.matcher(messageText)
        val unitMatcher = Patterns.unitBgPattern.matcher(messageText)

        var numberString = ""
        var unitString = ""

        if (unitMatcher.matches()) {
            numberString = unitMatcher.group(4)
            unitString = unitMatcher.group(5)
        }

        if (inlineMatcher.matches()) {
            numberString = inlineMatcher.group(1)
        } else if (separateMatcher.matches()) {
            numberString = separateMatcher.group(1)
        }

        if (numberString.isEmpty()) {
            return
        }

        try {
            val result = if (unitString.length > 1) {
                BGConversionFormatter.getResponse(numberString, unitString)
            } else {
                BGConversionFormatter.getResponse(numberString, null)
            }

            val sender = JDACommandUser.of(event, jdaCommandUpdateHandler)

            sender.reply(result.first)
                    .doOnSuccess { newMessage ->
                        addReactions(newMessage, result.second)

                        Pages.buttonize(
                                newMessage,
                                mapOf(Pages.getPaginator().emotes[Emote.CANCEL] to cancelButton),
                                true,
                                30,
                                TimeUnit.SECONDS,
                                Predicate.isEqual(message.author),
                                Consumer { addReactions(it, result.second) }
                        )
                    }
                    .subscribe()
        } catch (ex: IllegalArgumentException) {
            // Ignored on purpose
        } catch (ex: UnknownUnitException) {
            // Ignored on purpose
        }
    }

    private fun addReactions(message: Message, reactions: List<String>) {
        reactions.forEach {
            message.addReaction(it).queue()
        }
    }
}
