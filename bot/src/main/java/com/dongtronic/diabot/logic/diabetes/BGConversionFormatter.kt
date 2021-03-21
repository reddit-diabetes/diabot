package com.dongtronic.diabot.logic.diabetes

import com.dongtronic.diabot.data.ConversionDTO

/**
 * Formatter for blood glucose conversions
 */
object BGConversionFormatter {
    /**
     * Generate a glucose conversion response.
     *
     * @param value The blood glucose value to convert from
     * @param unit Optional: The measurement unit [value] is in
     * @return A pair with the response message and a [List] of reaction emojis for the conversion.
     */
    fun getResponse(value: String, unit: String? = null): Pair<String, List<String>> {
        val result = BloodGlucoseConverter.convert(value, unit)

        val reply = when {
            result.inputUnit === GlucoseUnit.MMOL -> String.format("%s mmol/L is %s mg/dL", result.mmol, result.mgdl)
            result.inputUnit === GlucoseUnit.MGDL -> String.format("%s mg/dL is %s mmol/L", result.mgdl, result.mmol)
            else -> {
                String.format(
                        arrayOf(
                                "*I'm not sure if you gave me mmol/L or mg/dL, so I'll give you both.*",
                                "%s mg/dL is **%s mmol/L**",
                                "%s mmol/L is **%s mg/dL**"
                        ).joinToString("%n"),
                        value,
                        result.mmol,
                        value,
                        result.mgdl
                )
            }
        }

        val reaction = getReactions(result)

        return reply to reaction
    }

    /**
     * Get the reaction emojis that should apply to the given [ConversionDTO].
     *
     * @param conversionDTO The glucose conversion result
     * @return A [List] of emojis which fit the glucose value.
     */
    fun getReactions(conversionDTO: ConversionDTO): List<String> {
        val reactions = mutableListOf<String>()

        // #20: Reply with :smirk: when value is 69 mg/dL or 6.9 mmol/L
        if (conversionDTO.mmol == 6.9 || conversionDTO.mgdl == 69) {
            reactions.add("\uD83D\uDE0F")
        }

        // #36 and #60: Reply with :100: when value is 100 mg/dL, 5.5 mmol/L, or 10.0 mmol/L
        if (conversionDTO.mmol == 5.5
                || conversionDTO.mmol == 10.0
                || conversionDTO.mgdl == 100) {
            reactions.add("\uD83D\uDCAF")
        }

        return reactions
    }
}