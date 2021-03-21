package com.dongtronic.diabot.platforms.discord.commands.diabetes

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.exceptions.UnknownUnitException
import com.dongtronic.diabot.logic.diabetes.BGConversionFormatter
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger

class ConvertCommand {
    private val logger = logger()

    @CommandMethod("convert <value> [unit]")
    @CommandDescription("Convert blood glucose between mmol/L and mg/dL")
    @CommandCategory(Category.BG)
    @Example(["[convert] 5"])
    fun execute(
            sender: JDACommandUser,
            @Argument("value", description = "BG value")
            value: String,
            @Argument("unit", description = "Unit of BG value")
            unit: String?
    ) {
        val event = sender.event
        if (event.author.isBot) {
            return
        }

        try {
            val result = BGConversionFormatter.getResponse(value, unit)

            sender.reply(result.first)
                    .doOnSuccess { message ->
                        result.second.forEach {
                            message.addReaction(it).queue()
                        }
                    }
                    .subscribe()
        } catch (ex: IllegalArgumentException) {
            sender.replyErrorS("Could not convert: ${ex.message}")
        } catch (ex: UnknownUnitException) {
            sender.replyErrorS("I don't know how to convert from $unit")
            logger.warn("Unknown BG unit $unit")
        }
    }
}
