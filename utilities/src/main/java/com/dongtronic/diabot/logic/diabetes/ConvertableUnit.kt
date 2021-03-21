package com.dongtronic.diabot.logic.diabetes

/**
 * A (diabetes) measurement unit that is able to be converted/translated to one (or more) measurement unit(s)
 */
interface ConvertableUnit {
    /**
     * Convert a value between the internal units for an area of measurement.
     *
     * For glucose units, this function would convert between mg/dL and mmol/L.
     *
     * For A1c units, this function would convert between IFCC and DCCT units.
     *
     * @param value The source value to convert from
     * @param round Whether the result should be rounded or not (if applicable)
     * @return A [Number] for the opposite unit in the same measurement area.
     * @throws NotImplementedError If this function has not been implemented by a unit
     */
    fun convert(value: Number, round: Boolean = true): Number = throw NotImplementedError()

    /**
     * Convert a value between units for different areas of measurement.
     *
     * For glucose units, this function would convert between BG mg/dL or mmol/L units and A1c IFCC or DCCT units.
     *
     * For A1c units, this function would convert between A1c IFCC or DCCT units and BG mg/dL or mmol/L units.
     *
     * @param value The source value to convert from
     * @param unit The desired measurement unit to convert to
     * @param round Whether the result should be rounded or not (if applicable)
     * @return A [Number] for the given unit in a different measurement area.
     * @throws NotImplementedError If this function has not been implemented by a unit
     */
    fun convert(value: Number, unit: ConvertableUnit, round: Boolean = true): Number = throw NotImplementedError()
}