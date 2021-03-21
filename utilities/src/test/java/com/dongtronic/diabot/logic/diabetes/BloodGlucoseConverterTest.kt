package com.dongtronic.diabot.logic.diabetes

import com.dongtronic.diabot.data.ConversionDTO
import com.dongtronic.diabot.exceptions.UnknownUnitException
import com.dongtronic.diabot.logic.diabetes.GlucoseUnit
import org.junit.Assert
import org.junit.Test

class BloodGlucoseConverterTest {

    @Test
    @Throws(Exception::class)
    fun mmolWithUnit() {

        val actual = BloodGlucoseConverter.convert("5.5", "mmol")

        val expected = ConversionDTO(5.5, 99.0, GlucoseUnit.MMOL)

        Assert.assertEquals(expected, actual)
    }

    @Test
    @Throws(Exception::class)
    fun mgdlWithUnit() {
        val actual = BloodGlucoseConverter.convert("100", "mgdl")

        val expected = ConversionDTO(100.0, 5.6, GlucoseUnit.MGDL)

        Assert.assertEquals(expected, actual)
    }

    @Test
    @Throws(Exception::class)
    fun mmolWithoutUnit() {
        val actual = BloodGlucoseConverter.convert("5.5", "")

        val expected = ConversionDTO(5.5, 99.0, GlucoseUnit.MMOL)

        Assert.assertEquals(expected, actual)
    }

    @Test
    @Throws(Exception::class)
    fun mgdlWithoutUnit() {
        val actual = BloodGlucoseConverter.convert("100", "")

        val expected = ConversionDTO(100.0, 5.6, GlucoseUnit.MGDL)

        Assert.assertEquals(expected, actual)
    }

    @Test
    @Throws(Exception::class)
    fun negative() {
        val actual = BloodGlucoseConverter.convert("-5.5", "mmol")

        val expected = ConversionDTO(-5.5, -99.0, GlucoseUnit.MMOL)

        Assert.assertEquals(expected, actual)
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun tooHigh() {
        BloodGlucoseConverter.convert("1000", "mmol")
    }


    @Test(expected = UnknownUnitException::class)
    @Throws(Exception::class)
    fun invalidUnit() {
        BloodGlucoseConverter.convert("5.5", "what")
    }

    @Test
    @Throws(Exception::class)
    fun ambiguous() {
        val actual = BloodGlucoseConverter.convert("27", "")

        val expected = ConversionDTO(27.0, 1.5, 486)

        Assert.assertEquals(actual, expected)
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun noInput() {
        BloodGlucoseConverter.convert("", "")
    }
}