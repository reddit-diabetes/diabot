package com.dongtronic.diabot.platforms.discord.commands.misc

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.Cooldown
import com.dongtronic.diabot.commands.annotations.DisplayName
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.commands.cooldown.CooldownScope
import com.dongtronic.diabot.exceptions.RequestStatusException
import com.dongtronic.diabot.logic.nutrition.NutritionixCommunicator
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger
import net.dv8tion.jda.api.EmbedBuilder
import reactor.core.scheduler.Schedulers
import java.util.concurrent.TimeUnit

class NutritionCommand {
    private val logger = logger()

    @CommandMethod("nutrition|nutriets <items>")
    @CommandDescription("Get nutrition information")
    @CommandCategory(Category.UTILITIES)
    @Example(["[nutrition] one slice of bread", "[nutrition] 1 slices brown bread with jam, 1 cup milk, 2 cups green salad"])
    @Cooldown(30, TimeUnit.SECONDS, CooldownScope.USER)
    fun execute(
            sender: JDACommandUser,
            @DisplayName("food item(s)")
            @Argument("items")
            @Greedy
            items: String
    ) {
        NutritionixCommunicator.getNutritionInfo(items)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe({ result ->
                    val builder = EmbedBuilder()

                    builder.setTitle("Nutrition Result")

                    builder.appendDescription("**Input**: $items \n")
                    builder.appendDescription("**Parsed**: \n$result")

                    if (result.totalCarbs.toInt() != 0) {
                        builder.addField("Carbs", result.totalCarbs.toInt().toString(), true)
                    }

                    if (result.totalFats.toInt() != 0) {
                        builder.addField("Total Fats", result.totalFats.toInt().toString(), true)
                    }

                    if (result.totalSaturatedFats.toInt() != 0) {
                        builder.addField("Saturated fats", result.totalSaturatedFats.toInt().toString(), true)
                    }

                    if (result.totalFibers.toInt() != 0) {
                        builder.addField("Dietary fibers", result.totalFibers.toInt().toString(), true)
                    }

                    builder.setColor(java.awt.Color.blue)
                    builder.setAuthor("Powered by Nutritionix")

                    sender.reply(builder.build()).subscribe()
                }, {
                    logger.warn("Could not communicate with Nutritionix", it)
                    if (it is RequestStatusException) {
                        when (it.status) {
                            404 -> {
                                sender.replyErrorS("Couldn't find any food matching your request in the food database")
                            }
                            401 -> {
                                sender.replyErrorS("Rate limit reached, please try again tomorrow")
                            }
                            else -> {
                                sender.replyErrorS("Couldn't communicate with nutrition database. Please try again later.")
                            }
                        }
                    } else {
                        sender.replyErrorS("An error occurred while grabbing nutrition data. Please try again later.")
                    }
                })
    }
}
