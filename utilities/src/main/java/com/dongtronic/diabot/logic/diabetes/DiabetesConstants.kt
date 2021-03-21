package com.dongtronic.diabot.logic.diabetes

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * A collection of diabetes-related constants/tools for unit conversions
 */
object DiabetesConstants {
    // Conversion factor between mg/dL and mmol/L measurements for glucose molecules.
    //
    // Glucose's molecular formula is C6H12O6. The total weight of this is 180.156 grams/mole.
    // This means that 180.156 grams of a glucose molecule is equal to 1 mole, allowing us to convert between
    // grams (weight) and moles (fixed number of particles).
    //
    // I would recommend looking at the following SI prefixes if you aren't familiar with them already, as they
    // are used in this explanation. I will try my best to keep the explanation simple, but it might be handy to
    // refer back to this chart throughout the reading:
    //
    // Prefix  | Decimal Factor | Power Factor
    // ---------------------------------------
    // NOTHING-| 1              | 10^0
    // deci-   | 0.1            | 10^-1
    // milli-  | 0.001          | 10^-3
    //
    // The mg/dL measurement can be converted directly from milli-grams of glucose to milli-moles of glucose because
    // mmol/L uses the same SI prefix and we know that converting grams to and from moles has a linear relationship,
    // therefore no conversion is necessary.
    //
    // The last remaining difference between mg/dL and mmol/L is the volume measurement (liter/litre):
    //
    // mg/dL uses deci-liters which means that 1 mg/dL = 0.1 mg/L.
    //
    // You can convert from deci-liters to liters by multiplying the mg/dL value by the power factor for 'deci-' in
    // the chart above:
    // 1 mg/dL = (1 × 10^-1) mg/L = 0.1 mg/L
    //
    // You can do the opposite and convert from liters to deci-liters by dividing instead of multiplying:
    // 0.1 mg/L = (0.1 ÷ 10^-1) mg/dL = 1 mg/dL
    //
    // Now that we know how to convert the liters into deciliters and vice-versa, we can apply it to mg/dL and mmol/L.
    // Remember, we don't need to convert 'mg' and 'mmol' since they're using the same base prefix.
    //
    // For now, I'm going to do two things to help make it easier to see what's happening:
    // - I'll use a larger value to convert from
    // - I will substitute our gram <-> mole conversion factor with `C` (for 'conversion factor')
    //
    // 99 mg/dL = (99 ÷ 10^-1) ÷ C mmol/L
    //
    // This looks confusing, but let me break it down. I'll start with the parenthesis first:
    // (99 ÷ 10^-1)
    // We need to convert from deciliters to liters, so we are dividing by the deci- conversion factor.
    // This effectively gives the value in mg/L, which is 990 mg/L.
    //
    // The next step is to convert from (milli)grams to (milli)moles, which is what C is for. Remember that C is
    // measured in C grams per mole, so if we have grams and we want to get how many moles makes up our grams, we use
    // division:
    // 990 ÷ C
    //
    // C's exact value (explained at the start of this comment) is 180.156, so replacing C with that makes the equation:
    // 990 ÷ 180.156
    //
    // And with that, my calculator gives me approximately 5.49 as the result. We measure mmol/L for blood glucose with
    // one decimal place, so rounding the result gives us 5.5 mmol/L as the final result. Confirming this with an
    // online calculator for blood glucose confirms our answer, which means we have successfully converted glucose units!
    //
    // And to make a equation for our process, we can plug in the value of `C` (it was only there to reduce the amount
    // of numbers in the equation at first glance) and replace our mg/dL value with a variable, such as `G`:
    // `G mg/dL = (G ÷ 10^-1) ÷ 180.156 mmol/L`
    //
    // We're not done though. As nice as it seems, we're having to do quite a bit of work to convert measurement units.
    // We can simplify the equation by combining the division steps into one.
    //
    // To do this, multiply `10^-1` and `180.156` together. The result of this is `18.0156`, which may look familiar.
    // This becomes our new value to divide by when converting from mg/dL to mmol/L.
    //
    // For converting from mmol/L to mg/dL, we just need to switch from division to multiplication. Again, our
    // conversion factor of `180.156` is how many grams makes up one mole, so if we want to get how many grams is in a
    // number of moles, we multiply by that grams/mole factor instead of dividing by it.
    //
    // Finally, we come to the final equation:
    // `G mg/dL = G ÷ 18.0156 mmol/L`
    // And lastly, for mmol/L to mg/dL (with `M` representing our mmol/L value):
    // `M mmol/L = M × 18.0156 mg/dL`
    //
    const val glucoseConversionFactor = 18.0156

    // Average Glucose formulas
    // From: https://care.diabetesjournals.org/content/diacare/early/2008/06/07/dc08-0545.full.pdf

    // DCCT -> BG

    fun dcctToMgdl(dcct: Double): Int {
        return dcctToMgdlDouble(dcct, 3).roundToInt()
    }

    fun dcctToMgdlDouble(dcct: Double, roundingPrecision: Int? = null): Double {
        return round((28.7 * dcct - 46.7), roundingPrecision)
    }

    fun dcctToMmol(dcct: Double, roundingPrecision: Int? = 1): Double {
        return mgdlToMmol(dcctToMgdlDouble(dcct, 3), roundingPrecision)
    }

    // IFCC -> BG

    fun ifccToMgdl(ifcc: Double): Int = dcctToMgdl(ifccToDcct(ifcc, null))

    fun ifccToMmol(ifcc: Double, roundingPrecision: Int? = 1): Double =
            dcctToMmol(ifccToDcct(ifcc, null), roundingPrecision)

    // BG -> DCCT

    fun mgdlToDcct(mgdl: Int, roundingPrecision: Int? = 1): Double {
        return round((mgdl + 46.7) / 28.7, roundingPrecision)
    }

    fun mmolToDcct(mmol: Double, roundingPrecision: Int? = 1): Double {
        return round((mmolToMgdlDouble(mmol) + 46.7) / 28.7, roundingPrecision)
    }

    // BG -> IFCC

    fun mgdlToIfcc(mgdl: Int, roundingPrecision: Int? = 1): Double =
            dcctToIfcc(mgdlToDcct(mgdl, null), roundingPrecision)

    fun mmolToIfcc(mmol: Double, roundingPrecision: Int? = 1): Double =
            dcctToIfcc(mmolToDcct(mmol, null), roundingPrecision)

    // Glucose conversions

    fun mgdlToMmol(mgdl: Int, roundingPrecision: Int? = 1): Double =
            round(mgdl / glucoseConversionFactor, roundingPrecision)

    fun mgdlToMmol(mgdl: Double, roundingPrecision: Int? = 1): Double =
            round(mgdl / glucoseConversionFactor, roundingPrecision)

    fun mmolToMgdl(mmol: Double): Int = (mmol * glucoseConversionFactor).roundToInt()

    fun mmolToMgdlDouble(mmol: Double, roundingPrecision: Int? = 3): Double =
            round(mmol * glucoseConversionFactor, roundingPrecision)

    // A1c conversions
    // From: http://www.ngsp.org/docs/IFCCstd.pdf

    fun dcctToIfcc(dcct: Double, roundingPrecision: Int? = 1): Double {
        return round((dcct - 2.152) / 0.09148, roundingPrecision)
    }

    fun ifccToDcct(ifcc: Double, roundingPrecision: Int? = 1): Double {
        return round((0.09148 * ifcc) + 2.152, roundingPrecision)
    }

    // Rounding
    // Technically this is not a 'diabetic constant' but it's still handy and is commonly required when displaying
    // BG or A1c values.
    fun round(value: Double, precision: Int? = 1): Double {
        return round(value as Number, precision) as Double
    }

    fun round(value: Number, precision: Int? = 1): Number {
        return if (value is Int || precision == null) {
            value
        } else {
            val factor = 10.0.pow(precision.toDouble())
            return (value.toDouble() * factor).roundToInt() / factor
        }
    }
}