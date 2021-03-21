package com.dongtronic.diabot.data

/**
 * Generic DTO for A1c conversions or estimates.
 */
interface A1cDTO {
    /**
     * The A1c in IFCC units
     */
    val ifcc: Double

    /**
     * The A1c in DCCT units
     */
    val dcct: Double
}