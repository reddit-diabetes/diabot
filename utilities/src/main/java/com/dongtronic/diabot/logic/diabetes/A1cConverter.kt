package com.dongtronic.diabot.logic.diabetes

import com.dongtronic.diabot.data.A1cFromBgDTO
import com.dongtronic.diabot.data.A1cToBgDTO
import com.dongtronic.diabot.data.ConversionDTO
import com.dongtronic.diabot.exceptions.UnknownUnitException

/**
 * A1c conversion logic
 */
object A1cConverter {

    /**
     * Convert a blood glucose value to an estimated A1c value.
     *
     * @param bgValue The blood glucose value to estimate A1c from.
     * @param bgUnit Optional: The measurement unit that [bgValue] is in. If this is null then the unit will be guessed.
     * @return [A1cFromBgDTO] with the estimated A1c value
     * @throws IllegalArgumentException If the glucose value given was not numeric
     * @throws IllegalArgumentException If the glucose value is not between `-999` and `999`
     * @throws UnknownUnitException If the measurement unit is not a valid unit
     * @see BloodGlucoseConverter.convert
     */
    fun a1cFromBg(bgValue: String, bgUnit: String? = null): A1cFromBgDTO {
        val glucoseConversionResult = BloodGlucoseConverter.convert(bgValue, bgUnit)

        return a1cFromBg(glucoseConversionResult)
    }

    /**
     * Convert a blood glucose value into an estimated A1c value.
     *
     * This function will defer to [estimateA1cAmbiguous] if the [glucose] [ConversionDTO.inputUnit] is [GlucoseUnit.AMBIGUOUS].
     *
     * @param glucose The [ConversionDTO] of the blood glucose value
     * @return [A1cFromBgDTO] containing an A1c estimate for the given glucose value
     * @see estimateA1cAmbiguous
     */
    fun a1cFromBg(glucose: ConversionDTO): A1cFromBgDTO {
        if (glucose.inputUnit == GlucoseUnit.AMBIGUOUS) {
            return estimateA1cAmbiguous(glucose)
        }

        val glucoseValue: Number = when (glucose.inputUnit) {
            GlucoseUnit.MGDL -> glucose.mgdl
            GlucoseUnit.MMOL -> glucose.mmol
            else -> return estimateA1cAmbiguous(glucose)
        }

        val pair: Pair<Double, Double> = a1cPairFromBg(glucoseValue, glucose.inputUnit)

        return A1cFromBgDTO(glucose, pair.first, pair.second)
    }

    /**
     * Convert an A1c value (in either IFCC or DCCT units) to an estimated average blood glucose.
     *
     * @param inputA1c The A1c value to convert from.
     * @param inputUnit Optional: The measurement unit that [inputA1c] is in. If this is null then the unit will be guessed.
     * @return [A1cToBgDTO] containing the estimated average blood glucose
     */
    fun a1cToBg(inputA1c: Double, inputUnit: A1cUnit? = null): A1cToBgDTO {
        val a1c = DiabetesConstants.round(inputA1c)
        val unit = when {
            inputUnit != null && inputUnit != A1cUnit.AMBIGUOUS -> inputUnit

            // guess the unit
            a1c < 25 -> A1cUnit.DCCT
            else -> A1cUnit.IFCC
        }

        val mgdl = unit.convert(a1c, GlucoseUnit.MGDL, false)

        val ifcc: Double
        val dcct: Double
        when (unit) {
            A1cUnit.IFCC -> {
                ifcc = a1c
                dcct = unit.convert(a1c).toDouble()
            }
            A1cUnit.DCCT -> {
                ifcc = unit.convert(a1c).toDouble()
                dcct = a1c
            }
            // this should never be called
            else -> throw IllegalArgumentException("A1c unit cannot be ambiguous")
        }

        val conversion = BloodGlucoseConverter.convertExplicit(mgdl, GlucoseUnit.MGDL)

        return A1cToBgDTO(conversion, ifcc, dcct)
    }

    /**
     * Convert a blood glucose value in ambiguous units into two sets of possible A1c values, one for each of the units
     * that the blood glucose value might be.
     *
     * @param glucose The [ConversionDTO] of the ambiguous-unit glucose value
     * @return [A1cFromBgDTO] containing two A1c estimates, one for each glucose unit (mg/dL and mmol/L)
     */
    private fun estimateA1cAmbiguous(glucose: ConversionDTO): A1cFromBgDTO {
        val mgdlPair = a1cPairFromBg(glucose.original, GlucoseUnit.MGDL)
        val mmolPair = a1cPairFromBg(glucose.original, GlucoseUnit.MMOL)

        return A1cFromBgDTO(glucose, mgdlPair.first, mgdlPair.second, mmolPair.first, mmolPair.second)
    }

    /**
     * Convert a blood glucose value into both IFCC and DCCT estimates.
     *
     * @param value The blood glucose value to estimate A1c from
     * @param unit The measurement unit that [value] is in
     * @return IFCC and DCCT A1c estimates for the given glucose value
     */
    private fun a1cPairFromBg(value: Number, unit: GlucoseUnit): Pair<Double, Double> {
        val ifcc: Double = unit.convert(value, A1cUnit.IFCC).toDouble()
        val dcct: Double = unit.convert(value, A1cUnit.DCCT).toDouble()

        return ifcc to dcct
    }
}
