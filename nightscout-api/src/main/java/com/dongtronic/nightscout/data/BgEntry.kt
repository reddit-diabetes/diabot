package com.dongtronic.nightscout.data

import com.dongtronic.diabot.data.ConversionDTO
import com.dongtronic.nightscout.TrendArrow
import java.time.Instant

data class BgEntry(
        val glucose: ConversionDTO,
        val delta: ConversionDTO? = null,
        val dateTime: Instant,
        val trend: TrendArrow = TrendArrow.NONE
) {
    fun newBuilder(): Builder = Builder(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BgEntry

        if (glucose != other.glucose) return false
        if (dateTime != other.dateTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = glucose.hashCode()
        result = 31 * result + dateTime.hashCode()
        return result
    }

    class Builder {
        private var glucose: ConversionDTO? = null
        private var delta: ConversionDTO? = null
        private var dateTime: Instant? = null
        private var trend: TrendArrow = TrendArrow.NONE

        constructor()

        constructor(bgEntry: BgEntry) {
            this.glucose = bgEntry.glucose
            this.delta = bgEntry.delta
            this.dateTime = bgEntry.dateTime
            this.trend = bgEntry.trend
        }

        fun glucose(glucose: ConversionDTO) = apply { this.glucose = glucose }

        fun delta(delta: ConversionDTO) = apply { this.delta = delta }

        fun dateTime(dateTime: Instant) = apply { this.dateTime = dateTime }

        fun trend(trend: TrendArrow) = apply { this.trend = trend }

        fun build() = BgEntry(
                glucose = checkNotNull(glucose) { "glucose == null" },
                delta = delta,
                dateTime = checkNotNull(dateTime) { "dateTime == null" },
                trend = trend
        )
    }
}