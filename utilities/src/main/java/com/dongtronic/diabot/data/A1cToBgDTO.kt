package com.dongtronic.diabot.data

/**
 * Estimated BG average from A1c
 *
 * @param bgAverage The blood glucose average
 * @param ifcc The A1c in IFCC units
 * @param dcct The A1c in DCCT units
 */
data class A1cToBgDTO(
        val bgAverage: ConversionDTO,
        override val ifcc: Double,
        override val dcct: Double
) : A1cDTO