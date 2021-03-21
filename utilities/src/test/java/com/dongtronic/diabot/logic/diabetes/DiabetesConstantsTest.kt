package com.dongtronic.diabot.logic.diabetes

import org.junit.Assert.assertEquals
import org.junit.Test

class DiabetesConstantsTest {

    private fun test(expected: Number, actual: () -> Number) {
        val actualValue = actual()

        assertEquals(expected, actualValue)
    }

    @Test
    fun dcctToMgdl() {
        test(97) { DiabetesConstants.dcctToMgdl(5.0) }
        test(126) { DiabetesConstants.dcctToMgdl(6.0) }
        test(154) { DiabetesConstants.dcctToMgdl(7.0) }
        test(183) { DiabetesConstants.dcctToMgdl(8.0) }
        test(212) { DiabetesConstants.dcctToMgdl(9.0) }
        test(240) { DiabetesConstants.dcctToMgdl(10.0) }
        test(269) { DiabetesConstants.dcctToMgdl(11.0) }
        test(298) { DiabetesConstants.dcctToMgdl(12.0) }
    }

    @Test
    fun dcctToMmol() {
        test(5.4) { DiabetesConstants.dcctToMmol(5.0) }
        test(7.0) { DiabetesConstants.dcctToMmol(6.0) }
        test(8.6) { DiabetesConstants.dcctToMmol(7.0) }
        test(10.2) { DiabetesConstants.dcctToMmol(8.0) }
        test(11.7) { DiabetesConstants.dcctToMmol(9.0) }
        test(13.3) { DiabetesConstants.dcctToMmol(10.0) }
        test(14.9) { DiabetesConstants.dcctToMmol(11.0) }
        test(16.5) { DiabetesConstants.dcctToMmol(12.0) }
    }

    @Test
    fun ifccToMgdl() {
        test(96) { DiabetesConstants.ifccToMgdl(31.0) }
        test(125) { DiabetesConstants.ifccToMgdl(42.0) }
        test(154) { DiabetesConstants.ifccToMgdl(53.0) }
        test(183) { DiabetesConstants.ifccToMgdl(64.0) }
        test(212) { DiabetesConstants.ifccToMgdl(75.0) }
        test(241) { DiabetesConstants.ifccToMgdl(86.0) }
        test(270) { DiabetesConstants.ifccToMgdl(97.0) }
        test(299) { DiabetesConstants.ifccToMgdl(108.0) }
    }

    @Test
    fun ifccToMmol() {
        test(5.4) { DiabetesConstants.ifccToMmol(31.0) }
        test(7.0) { DiabetesConstants.ifccToMmol(42.0) }
        test(8.6) { DiabetesConstants.ifccToMmol(53.0) }
        test(10.2) { DiabetesConstants.ifccToMmol(64.0) }
        test(11.8) { DiabetesConstants.ifccToMmol(75.0) }
        test(13.4) { DiabetesConstants.ifccToMmol(86.0) }
        test(15.0) { DiabetesConstants.ifccToMmol(97.0) }
        test(16.6) { DiabetesConstants.ifccToMmol(108.0) }
    }

    @Test
    fun mgdlToDcct() {
        test(5.0) { DiabetesConstants.mgdlToDcct(97) }
        test(6.0) { DiabetesConstants.mgdlToDcct(126) }
        test(7.0) { DiabetesConstants.mgdlToDcct(154) }
        test(8.0) { DiabetesConstants.mgdlToDcct(183) }
        test(9.0) { DiabetesConstants.mgdlToDcct(212) }
        test(10.0) { DiabetesConstants.mgdlToDcct(240) }
        test(11.0) { DiabetesConstants.mgdlToDcct(269) }
        test(12.0) { DiabetesConstants.mgdlToDcct(298) }
    }

    @Test
    fun mmolToDcct() {
        test(5.0) { DiabetesConstants.mmolToDcct(5.4) }
        test(6.0) { DiabetesConstants.mmolToDcct(7.0) }
        test(7.0) { DiabetesConstants.mmolToDcct(8.6) }
        test(8.0) { DiabetesConstants.mmolToDcct(10.2) }
        test(9.0) { DiabetesConstants.mmolToDcct(11.7) }
        test(10.0) { DiabetesConstants.mmolToDcct(13.3) }
        test(11.0) { DiabetesConstants.mmolToDcct(14.9) }
        test(12.0) { DiabetesConstants.mmolToDcct(16.5) }
    }

    @Test
    fun mgdlToIfcc() {
        test(31.2) { DiabetesConstants.mgdlToIfcc(97) }
        test(42.3) { DiabetesConstants.mgdlToIfcc(126) }
        test(52.9) { DiabetesConstants.mgdlToIfcc(154) }
        test(64.0) { DiabetesConstants.mgdlToIfcc(183) }
        test(75.0) { DiabetesConstants.mgdlToIfcc(212) }
        test(85.7) { DiabetesConstants.mgdlToIfcc(240) }
        test(96.7) { DiabetesConstants.mgdlToIfcc(269) }
        test(107.8) { DiabetesConstants.mgdlToIfcc(298) }
    }

    @Test
    fun mmolToIfcc() {
        test(31.3) { DiabetesConstants.mmolToIfcc(5.4) }
        test(42.3) { DiabetesConstants.mmolToIfcc(7.0) }
        test(53.3) { DiabetesConstants.mmolToIfcc(8.6) }
        test(64.3) { DiabetesConstants.mmolToIfcc(10.2) }
        test(75.2) { DiabetesConstants.mmolToIfcc(11.8) }
        test(86.2) { DiabetesConstants.mmolToIfcc(13.4) }
        test(96.5) { DiabetesConstants.mmolToIfcc(14.9) }
        test(107.5) { DiabetesConstants.mmolToIfcc(16.5) }
    }

    @Test
    fun mgdlToMmol() {
        // integer input
        test(2.7) { DiabetesConstants.mgdlToMmol(49) }
        test(4.0) { DiabetesConstants.mgdlToMmol(72) }
        test(6.5) { DiabetesConstants.mgdlToMmol(117) }
        test(6.9) { DiabetesConstants.mgdlToMmol(124) }
        test(6.9) { DiabetesConstants.mgdlToMmol(125) }
        test(22.2) { DiabetesConstants.mgdlToMmol(400) }

        // double input
        test(1.3) { DiabetesConstants.mgdlToMmol(23.42028) }
        test(4.0) { DiabetesConstants.mgdlToMmol(72.0624) }
        test(6.5) { DiabetesConstants.mgdlToMmol(117.1014) }
        test(6.9) { DiabetesConstants.mgdlToMmol(124.30764) }
        test(7.0) { DiabetesConstants.mgdlToMmol(125.7815) }
        test(22.2) { DiabetesConstants.mgdlToMmol(399.94632) }
    }

    @Test
    fun mmolToMgdl() {
        // integer ouput
        test(99) { DiabetesConstants.mmolToMgdl(5.5) }
        test(104) { DiabetesConstants.mmolToMgdl(5.8) }
        test(105) { DiabetesConstants.mmolToMgdl(5.81) }
        test(124) { DiabetesConstants.mmolToMgdl(6.9) }
        test(126) { DiabetesConstants.mmolToMgdl(7.0) }
        test(721) { DiabetesConstants.mmolToMgdl(40.0) }

        // double output
        test(99.086) { DiabetesConstants.mmolToMgdlDouble(5.5) }
        test(104.490) { DiabetesConstants.mmolToMgdlDouble(5.8) }
        test(104.671) { DiabetesConstants.mmolToMgdlDouble(5.81) }
        test(124.308) { DiabetesConstants.mmolToMgdlDouble(6.9) }
        test(126.109) { DiabetesConstants.mmolToMgdlDouble(7.0) }
        test(720.624) { DiabetesConstants.mmolToMgdlDouble(40.0) }
    }

    @Test
    fun dcctToIfcc() {
        test(37.7) { DiabetesConstants.dcctToIfcc(5.6) }
        test(44.3) { DiabetesConstants.dcctToIfcc(6.2) }
        test(53.0) { DiabetesConstants.dcctToIfcc(7.0) }
        test(65.0) { DiabetesConstants.dcctToIfcc(8.1) }
        test(151.4) { DiabetesConstants.dcctToIfcc(16.0) }
    }

    @Test
    fun ifccToDcct() {
        test(5.0) { DiabetesConstants.ifccToDcct(31.2) }
        test(6.8) { DiabetesConstants.ifccToDcct(50.7) }
        test(11.2) { DiabetesConstants.ifccToDcct(99.0) }
        test(11.3) { DiabetesConstants.ifccToDcct(99.99) }
        test(11.5) { DiabetesConstants.ifccToDcct(102.6) }
        test(13.1) { DiabetesConstants.ifccToDcct(120.0) }
        test(18.7) { DiabetesConstants.ifccToDcct(180.4) }
    }
}