package com.dongtronic.diabot.graph

import com.dongtronic.diabot.logic.diabetes.GlucoseUnit
import com.dongtronic.diabot.util.logger
import com.dongtronic.nightscout.data.BgEntry

object ScalingUtil {
    private val logger = logger()

    /**
     * Finds a minimum and maximum Y-point for having an acceptable graph scale.
     * This ensures that BGs on near-flat deltas over several hours won't have their graphs zoomed in and seem jittery.
     * Currently, this function is locked to 54mg/dL / 3.0mmol/L as the minimum graph range.
     *
     * @param entries The BG entries to find min and max points for
     * @param unit The glucose units to display the min and max points in
     * @return The minimum and maximum Y-points, in the glucose units provided
     */
    fun findMinMax(entries: List<BgEntry>, unit: GlucoseUnit): Pair<Double, Double> {
        var min = Double.MAX_VALUE
        var max = Double.MIN_VALUE

        entries.forEach { entry ->
            val dataPoint = entry.glucose.mgdl.toDouble()

            if (!dataPoint.isNaN()) {
                if (dataPoint < min) {
                    min = dataPoint
                }
                if (dataPoint > max) {
                    max = dataPoint
                }
            }
        }

        // using 54mg since it divides evenly into 3mmol. probably not needed, but it's whatever
        val scale = 54.0
        if (max - min < scale) {
            val expanded = expandMinMax(minMax = min to max, limits = 0.0 to null, size = scale)
            min = expanded.first
            max = expanded.second
        }

        if (unit != GlucoseUnit.MGDL) {
            min /= 18.0156
            max /= 18.0156
        }

        return Pair(min, max)
    }

    /**
     * Expands a min-max range (the distance from the min and max points) to a specific size, while limiting them to a specific range as well.
     * The minimum point will always expand in the negative direction, and the maximum point will always expand in the positive direction.
     *
     * @param minMax The starting minimum and maximum range values
     * @param limits Limits for the minimum and maximum range values.
     * The limits must not be at the size of the provided `minMax` or smaller.
     * @param size The desired size of the range. This must be greater than 0
     * @param step The value which the min and max points will be incremented/decremented by on each loop
     * @return The expanded minimum and maximum range values
     */
    private fun expandMinMax(minMax: Pair<Double, Double>, limits: Pair<Double?, Double?>, size: Double, step: Double = 1.0): Pair<Double, Double> {
        require(size > 0) { "The size of the range must be greater than 0" }
        if (limits.first != null)
            require(limits.first!! < minMax.first) { "The min limit cannot be bigger than the starting min point" }

        if (limits.second != null)
            require(limits.second!! > minMax.second) { "The max limit cannot be smaller than the starting max point" }

        var min = minMax.first
        val minLimit = limits.first
        var max = minMax.second
        val maxLimit = limits.second

        while (max - min < size) {
            val minAtLimit = minLimit?.let { min <= minLimit } ?: false
            val maxAtLimit = maxLimit?.let { max >= maxLimit } ?: false

            if (minAtLimit && maxAtLimit) {
                logger.warn("Can't expand range any more")
                break
            } else {
                if (!minAtLimit)
                    min-=step

                if (!maxAtLimit)
                    max+=step
            }
        }

        return min to max
    }
}