package com.dongtronic.diabot.logic.diabetes

enum class A1cUnit(
        vararg units: String,
        /**
         * Convert an A1c unit to the other A1c unit
         *
         * @see otherUnit
         */
        private val convertToOther: ((Number) -> Number) = { throw NotImplementedError() },

        /**
         * Estimate an average mg/dL BG value from an A1c value
         *
         * @see convert
         */
        private val convertToMgdl: ((Number) -> Number) = { throw NotImplementedError() },

        /**
         * Estimate an average mmol/L BG value from an A1c value
         *
         * @see convert
         */
        private val convertToMmol: ((Number) -> Number) = { throw NotImplementedError() }
) : ConvertableUnit {
    IFCC("mmol/mol",
            "mmol",
            "ifcc",
            convertToOther = { DiabetesConstants.ifccToDcct(it.toDouble()) },
            convertToMgdl = { DiabetesConstants.ifccToMgdl(it.toDouble()) },
            convertToMmol = { DiabetesConstants.ifccToMmol(it.toDouble()) }
    ),

    DCCT("%",
            "dcct",
            convertToOther = { DiabetesConstants.dcctToIfcc(it.toDouble()) },
            convertToMgdl = { DiabetesConstants.dcctToMgdl(it.toDouble()) },
            convertToMmol = { DiabetesConstants.dcctToMmol(it.toDouble()) }
    ),

    AMBIGUOUS("Ambiguous");

    /**
     * A list of names which identify this particular [A1cUnit]
     */
    val units: List<String> = units.toList()

    /**
     * Get the other [A1cUnit]. For example, if this unit is IFCC then this value will be DCCT.
     */
    val otherUnit = lazy {
        when (this) {
            IFCC -> DCCT
            DCCT -> IFCC
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
            is A1cUnit -> {
                // a1c <-> a1c
                this.convert(value)
            }
            is GlucoseUnit -> {
                // a1c -> bg
                when (unit) {
                    GlucoseUnit.MGDL -> this.convertToMgdl.invoke(value)
                    GlucoseUnit.MMOL -> this.convertToMmol.invoke(value)
                    GlucoseUnit.AMBIGUOUS -> throw IllegalStateException("Cannot convert to ambiguous glucose unit")
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
         * Attempts to find a [A1cUnit] which matches the unit name given.
         * This function does not match [A1cUnit.AMBIGUOUS].
         *
         * @param unit The name of the unit to look up
         * @return [A1cUnit] if a unit was matched, null if no matches were found
         */
        fun byName(unit: String): A1cUnit? {
            return values().firstOrNull { a1cUnit ->
                if (a1cUnit == AMBIGUOUS)
                    return@firstOrNull false

                a1cUnit.units.any { name ->
                    name.equals(unit, ignoreCase = true)
                }
            }
        }
    }
}
