package com.dongtronic.diabot.data

import com.dongtronic.diabot.logic.diabetes.DiabetesConstants.round
import com.dongtronic.diabot.logic.diabetes.GlucoseUnit

/**
 * Estimated A1c from blood glucose average
 *
 * @param inputGlucose The blood glucose average
 * @param ifccMgdl Estimated IFCC A1c value for the [inputGlucose]'s mg/dL value
 * @param dcctMgdl Estimated DCCT A1c value for the [inputGlucose]'s mg/dL value
 * @param ifccMmol Optional: Estimated IFCC A1c value for the [inputGlucose]'s mmol/L value. If this is not specified,
 * this will default to the value of [ifccMgdl].
 *
 * The purpose of this parameter is for ambiguous input glucose units. If the input was not ambiguous then this parameter
 * is not necessary to set.
 * @param dcctMmol Optional: Estimated DCCT A1c value for the [inputGlucose]'s mmol/L value. If this is not specified,
 * this will default to the value of [dcctMgdl].
 *
 * The purpose of this parameter is for ambiguous input glucose units. If the input was not ambiguous then this parameter
 * is not necessary to set.
 */
data class A1cFromBgDTO(
        val inputGlucose: ConversionDTO,
        val ifccMgdl: Double,
        val dcctMgdl: Double,

        val ifccMmol: Double = ifccMgdl,
        val dcctMmol: Double = dcctMgdl
) : A1cDTO {
    override val ifcc: Double
        get() = getIfcc()
    override val dcct: Double
        get() = getDcct()

    /**
     * Get the IFCC A1c estimate for the input glucose average.
     *
     * @param unit The glucose unit to estimate the A1c for. This is not necessary if the input conversion was not
     * ambiguous.
     * @return Estimated IFCC A1c value
     * @throws IllegalArgumentException If the [inputGlucose]'s unit is [GlucoseUnit.AMBIGUOUS] and [unit] was not overridden
     */
    fun getIfcc(unit: GlucoseUnit = inputGlucose.inputUnit): Double {
        return round(when (unit) {
            GlucoseUnit.MMOL -> ifccMmol
            GlucoseUnit.MGDL -> ifccMgdl
            else -> throw ambiguousException
        })
    }

    /**
     * Get the DCCT A1c estimate for the input glucose average.
     *
     * @param unit The glucose unit to estimate the A1c for. This is not necessary if the input conversion was not
     * ambiguous.
     * @return Estimated DCCT A1c value
     * @throws IllegalArgumentException If the [inputGlucose]'s unit is [GlucoseUnit.AMBIGUOUS] and [unit] was not overridden
     */
    fun getDcct(unit: GlucoseUnit = inputGlucose.inputUnit): Double {
        return round(when (unit) {
            GlucoseUnit.MMOL -> dcctMmol
            GlucoseUnit.MGDL -> dcctMgdl
            else -> throw ambiguousException
        })
    }

    companion object {
        private val ambiguousException = IllegalArgumentException("Cannot get A1c from ambiguous BG")
    }
}