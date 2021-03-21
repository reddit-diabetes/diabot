package com.dongtronic.diabot.logic.diabetes

enum class GlucoseUnit(
        vararg units: String,
        /**
         * Convert a glucose unit to the other glucose unit
         *
         * @see otherUnit
         */
        private val convertToOther: ((Number) -> Number) = { throw NotImplementedError() },

        /**
         * Estimate an IFCC A1c value from an average glucose value
         *
         * @see convert
         */
        private val convertToIfcc: ((Number) -> Double) = { throw NotImplementedError() },

        /**
         * Estimate an DCCT A1c value from an average glucose value
         *
         * @see convert
         */
        private val convertToDcct: ((Number) -> Double) = { throw NotImplementedError() }
) : ConvertableUnit {
    // nightscout uses "mmol/l" and "mmol"
    // https://github.com/nightscout/cgm-remote-monitor#required
    MMOL("mmol/L",
            "mmol",
            convertToOther = { DiabetesConstants.mmolToMgdl(it.toDouble()) },
            convertToIfcc = { DiabetesConstants.mmolToIfcc(it.toDouble()) },
            convertToDcct = { DiabetesConstants.mmolToDcct(it.toDouble()) }
    ),
    MGDL("mg/dL",
            "mgdl",
            "mg",
            convertToOther = { DiabetesConstants.mgdlToMmol(it.toInt()) },
            convertToIfcc = { DiabetesConstants.mgdlToIfcc(it.toInt()) },
            convertToDcct = { DiabetesConstants.mgdlToDcct(it.toInt()) }
    ),

    // this unit shouldn't be identified by name
    AMBIGUOUS("Ambiguous");

    /**
     * A list of names which identify this particular [GlucoseUnit]
     */
    val units: List<String> = units.toList()

    /**
     * Get the other [GlucoseUnit]. For example, if this unit is mg/dL then this value will be mmol/L.
     */
    val otherUnit = lazy {
        when (this) {
            MMOL -> MGDL
            MGDL -> MMOL
            AMBIGUOUS -> throw IllegalStateException("Ambiguous unit does not have an 'other' type")
        }
    }

    override fun convert(value: Number, round: Boolean): Number {
        val result = this.convertToOther.invoke(value)

        return if (round) {
            DiabetesConstants.round(result)
        } else {
            result
        }
    }

    override fun convert(value: Number, unit: ConvertableUnit, round: Boolean): Number {
        val result = when (unit) {
            is GlucoseUnit -> {
                // bg <-> bg
                this.convert(value)
            }
            is A1cUnit -> {
                // bg -> a1c

                when (unit) {
                    A1cUnit.IFCC -> this.convertToIfcc.invoke(value)
                    A1cUnit.DCCT -> this.convertToDcct.invoke(value)
                    A1cUnit.AMBIGUOUS -> throw IllegalStateException("Cannot convert to ambiguous A1c unit")
                }
            }
            else -> {
                throw NotImplementedError()
            }
        }

        return if (round) {
            DiabetesConstants.round(result)
        } else {
            result
        }
    }

    companion object {
        /**
         * Attempts to find a [GlucoseUnit] which matches the unit name given.
         * This function does not match [GlucoseUnit.AMBIGUOUS].
         *
         * @param unit The name of the unit to look up
         * @return [GlucoseUnit] if a unit was matched, null if no matches were found
         */
        fun byName(unit: String): GlucoseUnit? {
            return values().firstOrNull { glucoseUnit ->
                if (glucoseUnit == AMBIGUOUS)
                    return@firstOrNull false

                glucoseUnit.units.any { name ->
                    name.equals(unit, ignoreCase = true)
                }
            }
        }
    }
}
