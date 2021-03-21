package com.dongtronic.diabot.data

import com.dongtronic.diabot.logic.diabetes.DiabetesConstants
import com.dongtronic.diabot.logic.diabetes.GlucoseUnit

/**
 * A data object for converted blood glucose values.
 */
class ConversionDTO {
    /**
     * The blood glucose that the conversion occurred from
     */
    val original: Number

    /**
     * The measurement unit that the conversion occurred from
     */
    val inputUnit: GlucoseUnit


    /**
     * The converted mmol/L blood glucose value
     */
    val mmol: Double

    /**
     * The converted mg/dL blood glucose value
     */
    val mgdl: Int

    /**
     * Create a ConversionDTO object with explicit unit
     *
     * @param original   original BG value
     * @param conversion converted BG value
     * @param inputUnit  original unit
     * @throws IllegalArgumentException If the [inputUnit] is [GlucoseUnit.AMBIGUOUS]
     */
    constructor(original: Number, conversion: Number, inputUnit: GlucoseUnit) {
        this.original = DiabetesConstants.round(original)
        this.inputUnit = inputUnit

        when (inputUnit) {
            GlucoseUnit.MMOL -> {
                this.mgdl = conversion.toInt()
                this.mmol = this.original.toDouble()
            }
            GlucoseUnit.MGDL -> {
                this.mgdl = this.original.toInt()
                this.mmol = DiabetesConstants.round(conversion.toDouble())
            }
            else -> throw IllegalArgumentException("Single conversion constructor must contain explicit input unit")
        }
    }

    /**
     * Create a ConversionDTO object with ambiguous measurement unit.
     *
     * @param original       original BG value
     * @param mmolConversion BG value converted to mmol/L
     * @param mgdlConversion BG value converted to mg/dL
     */
    constructor(original: Number, mmolConversion: Double, mgdlConversion: Int) {
        this.original = DiabetesConstants.round(original)
        this.inputUnit = GlucoseUnit.AMBIGUOUS
        this.mmol = DiabetesConstants.round(mmolConversion)
        this.mgdl = mgdlConversion
    }

    /**
     * Get the converted BG value. This essentially returns the opposite unit of the input unit.
     *
     * MG/DL  input -> MMOL/L output
     *
     * MMOL/L input -> MG/DL  output
     *
     * @return The converted BG value.
     *
     * If the input was mg/dl, this will be a mmol/L [Double] value.
     *
     * If the input was mmol/L, this will be a mg/dL [Int] value.
     * @throws IllegalStateException If the input unit is ambiguous.
     * */
    fun getConverted(): Number {
        return when (inputUnit) {
            GlucoseUnit.MGDL -> this.mmol
            GlucoseUnit.MMOL -> this.mgdl
            GlucoseUnit.AMBIGUOUS ->
                throw IllegalStateException("Cannot retrieve converted BG result for ambiguous input")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConversionDTO

        if (original != other.original) return false
        if (inputUnit != other.inputUnit) return false
        if (mmol != other.mmol) return false
        if (mgdl != other.mgdl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = original.hashCode()
        result = 31 * result + inputUnit.hashCode()
        result = 31 * result + mmol.hashCode()
        result = 31 * result + mgdl
        return result
    }

    override fun toString(): String {
        return "ConversionDTO(original=$original, inputUnit=$inputUnit, mmol=$mmol, mgdl=$mgdl)"
    }
}