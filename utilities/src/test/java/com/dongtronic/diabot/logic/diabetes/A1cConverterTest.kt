package com.dongtronic.diabot.logic.diabetes

import com.dongtronic.diabot.data.A1cFromBgDTO
import com.dongtronic.diabot.data.A1cToBgDTO
import com.dongtronic.diabot.data.ConversionDTO
import org.junit.Assert.assertEquals
import org.junit.Test

class A1cConverterTest {
    // A1c -> BG

    @Test
    fun ambiguousIfccA1cToBg() {
        val actual = A1cConverter.a1cToBg(72.7)

        val expected = A1cToBgDTO(ConversionDTO(206, 11.4, GlucoseUnit.MGDL), 72.7, 8.8)

        assertEquals(expected, actual)
    }

    @Test
    fun ambiguousDcctA1cToBg() {
        val actual = A1cConverter.a1cToBg(6.2)

        val expected = A1cToBgDTO(ConversionDTO(131, 7.3, GlucoseUnit.MGDL), 44.3, 6.2)

        assertEquals(expected, actual)
    }

    @Test
    fun ifccA1cToBg() {
        val actual = A1cConverter.a1cToBg(32.4, A1cUnit.IFCC)

        val expected = A1cToBgDTO(ConversionDTO(100, 5.6, GlucoseUnit.MGDL), 32.4, 5.1)

        assertEquals(expected, actual)
    }

    @Test
    fun dcctA1cToBg() {
        val actual = A1cConverter.a1cToBg(8.0, A1cUnit.DCCT)

        val expected = A1cToBgDTO(ConversionDTO(183, 10.2, GlucoseUnit.MGDL), 63.9, 8.0)

        assertEquals(expected, actual)
    }

    // BG -> A1c

    @Test
    fun mgdlToA1c() {
        val bg = ConversionDTO(146, 8.1, GlucoseUnit.MGDL)

        val actual = A1cConverter.a1cFromBg(bg)

        val expected = A1cFromBgDTO(bg, 49.9, 6.7)

        assertEquals(expected, actual)
    }

    @Test
    fun mmolToA1c() {
        val bg = ConversionDTO(8.7, 157, GlucoseUnit.MMOL)

        val actual = A1cConverter.a1cFromBg(bg)

        val expected = A1cFromBgDTO(bg, 54.0, 7.1)

        assertEquals(expected, actual)
    }

    @Test
    fun ambiguousBgToA1c() {
        val bg = ConversionDTO(26, 1.4, 468)

        val actual = A1cConverter.a1cFromBg(bg)

        val expected = A1cFromBgDTO(bg, 4.2, 2.5, 172.7, 17.9)

        assertEquals(expected, actual)
    }
}