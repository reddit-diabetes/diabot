package com.dongtronic.diabot.logic.diabetes

import com.dongtronic.diabot.data.ConversionDTO
import com.dongtronic.diabot.exceptions.UnknownUnitException

/**
 * BG conversion logic
 */
object BloodGlucoseConverter {

    /**
     * Convert a blood glucose value and (optionally) a measurement unit from strings.
     *
     * @param value The blood glucose value as a [String]
     * @param unit Optional: The measurement unit for the blood glucose value as a [String]
     * @return A [ConversionDTO] for the given glucose value and measurement unit
     * @throws IllegalArgumentException If the glucose value given was not numeric
     * @throws IllegalArgumentException If the glucose value is not between `-999` and `999`
     * @throws UnknownUnitException If the measurement unit is not a valid unit
     * @see convertGuess
     * @see convertExplicit
     * @see convertAmbiguous
     */
    fun convert(value: String, unit: String? = null): ConversionDTO {
        val input = value.toDoubleOrNull()
                ?: throw IllegalArgumentException("Glucose value must be numeric")

        if (input < -999 || input > 999) {
            throw IllegalArgumentException("Glucose value must be between -999 and 999")
        }

        val glucoseUnit = GlucoseUnit.byName(unit ?: "")

        if (glucoseUnit == null && !unit.isNullOrBlank()) {
            throw UnknownUnitException()
        }

        return if (glucoseUnit != null) {
            convertExplicit(input, glucoseUnit)
        } else {
            convertGuess(input)
        }
    }

    /**
     * Convert a blood glucose value and attempt to guess the [GlucoseUnit].
     *
     * The guessing logic works as such:
     *
     * `if (value < 25) unit = MMOL`
     *
     * `if (value > 50) unit = MMOL`
     *
     * `else unit = AMBIGUOUS`
     *
     * @param originalValue a blood glucose value
     * @return A [ConversionDTO] for the given glucose value and a guessed [GlucoseUnit]
     */
    fun convertGuess(originalValue: Double): ConversionDTO {
        return when {
            originalValue < 25 -> convertExplicit(originalValue, GlucoseUnit.MMOL)
            originalValue > 50 -> convertExplicit(originalValue, GlucoseUnit.MGDL)
            else -> convertAmbiguous(originalValue)
        }
    }

    /**
     * Convert a blood glucose value from a defined [GlucoseUnit].
     *
     * @param value The blood glucose value to convert
     * @param unit The measurement unit for the given glucose value
     * @return A [ConversionDTO] for the given glucose value and unit
     */
    fun convertExplicit(value: Number, unit: GlucoseUnit): ConversionDTO {
        requireNonAmbiguous(unit)

        return ConversionDTO(value, unit.convert(value), unit)
    }

    /**
     * Convert a glucose value from an ambiguous measurement unit.
     *
     * In the [ConversionDTO], the given [originalValue] will be converted into both units:
     *
     * `mmol -> mgdl` and `mgdl -> mmol`
     *
     * This is done to avoid making the user define a measurement unit even if it means doing additional, possibly
     * unnecessary conversions.
     *
     * @param originalValue The blood glucose value to convert
     * @return A [ConversionDTO] for the given glucose value with both conversion directions
     */
    fun convertAmbiguous(originalValue: Double): ConversionDTO {
        val toMgdl = GlucoseUnit.MMOL.convert(originalValue).toInt()
        val toMmol = GlucoseUnit.MGDL.convert(originalValue).toDouble()

        return ConversionDTO(originalValue, toMmol, toMgdl)
    }

    /**
     * Helper function for requiring a [GlucoseUnit] object to not be [GlucoseUnit.AMBIGUOUS]
     *
     * @param glucoseUnit [GlucoseUnit] to ensure is not ambiguous
     */
    private fun requireNonAmbiguous(glucoseUnit: GlucoseUnit) =
            require(glucoseUnit != GlucoseUnit.AMBIGUOUS) { "Glucose unit cannot be ambiguous" }
}
