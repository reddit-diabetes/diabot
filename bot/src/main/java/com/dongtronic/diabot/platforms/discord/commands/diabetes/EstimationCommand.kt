package com.dongtronic.diabot.platforms.discord.commands.diabetes

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import com.dongtronic.diabot.commands.Category
import com.dongtronic.diabot.commands.annotations.CommandCategory
import com.dongtronic.diabot.commands.annotations.Example
import com.dongtronic.diabot.data.A1cFromBgDTO
import com.dongtronic.diabot.exceptions.UnknownUnitException
import com.dongtronic.diabot.logic.diabetes.A1cConverter
import com.dongtronic.diabot.logic.diabetes.A1cUnit
import com.dongtronic.diabot.logic.diabetes.GlucoseUnit
import com.dongtronic.diabot.logic.diabetes.GlucoseUnit.*
import com.dongtronic.diabot.platforms.discord.commands.JDACommandUser
import com.dongtronic.diabot.util.logger

class EstimationCommand {
    private val logger = logger()

    @CommandMethod("estimate average <a1c> [a1cUnit]")
    @CommandDescription("Estimate average blood glucose from an A1c value")
    @CommandCategory(Category.A1C)
    @Example(["[average] 6.7", "[average] 42", "[average] 65 ifcc", "[average] 8.0 dcct"])
    private fun estimateAverage(
            sender: JDACommandUser,
            @Argument("a1c", description = "The A1c value to estimate average blood glucose from")
            a1c: Double,
            @Argument("a1cUnit", description = "The measurement unit used for the A1c value")
            a1cUnit: String?
    ) {
        val unit = if (a1cUnit != null) {
            val fromName = A1cUnit.byName(a1cUnit)

            if (fromName == null) {
                sender.replyErrorS("No A1c unit is known by `$a1cUnit`. Please specify either `DCCT` or `IFCC`")
                return
            } else fromName
        } else null

        val result = try {
            A1cConverter.a1cToBg(a1c, unit)
        } catch (e: IllegalArgumentException) {
            sender.replyErrorS("Could not estimate average. Please make sure your A1c value is lower " +
                    "than **36.4%** (DCCT) **374 mmol/mol** (IFCC) and is not negative.")
            return
        }

        sender.replyS(String.format(
                "An A1c of **%s%%** (DCCT) or **%s mmol/mol** (IFCC) is about **%s mg/dL** or **%s mmol/L**",
                result.dcct,
                result.ifcc,
                result.bgAverage.mgdl,
                result.bgAverage.mmol
        ))
    }

    @CommandMethod("estimate a1c <bg> [bgUnit]")
    @CommandDescription("Estimate A1c from an average blood glucose value")
    @CommandCategory(Category.A1C)
    @Example(["[a1c] 120", "[a1c] 120 mg/dL", "[a1c] 5.7", "[a1c] 5.7 mmol"])
    private fun estimateA1c(
            sender: JDACommandUser,
            @Argument("bg", description = "The average BG value to estimate A1c from")
            bg: Double,
            @Argument("bgUnit", description = "The measurement unit used for the BG value")
            bgUnit: String?
    ) {
        try {
            val result = A1cConverter.a1cFromBg(bg.toString(), bgUnit)

            val response = if (result.inputGlucose.inputUnit == AMBIGUOUS) {
                buildString {
                    append("I'm not sure what unit your BG value is in, so I'll give A1c estimates for both units.\n")
                    append(generateA1cResponse(result, glucoseUnit = MMOL))
                    append("\n")
                    append(generateA1cResponse(result, glucoseUnit = MGDL))
                }
            } else {
                generateA1cResponse(result)
            }

            sender.reply(response)
        } catch (ex: IllegalArgumentException) {
            if (ex.message == null) {
                sender.replyErrorS("I could not convert from the BG you provided.")
            } else {
                sender.replyErrorS("Invalid argument: ${ex.message}")
            }
        } catch (ex: UnknownUnitException) {
            sender.replyErrorS("I don't know how to convert from $bgUnit")
        }
    }

    private fun generateA1cResponse(
            a1cDTO: A1cFromBgDTO,
            glucoseValue: Number = a1cDTO.inputGlucose.original,
            glucoseUnit: GlucoseUnit = a1cDTO.inputGlucose.inputUnit
    ): String {
        val format = "An average of %s %s is about **%s%%** (DCCT) or **%s mmol/mol** (IFCC)"
        val bgUnits = glucoseUnit.units.first()

        return String.format(format, glucoseValue, bgUnits, a1cDTO.getDcct(glucoseUnit), a1cDTO.getIfcc(glucoseUnit))
    }
}
