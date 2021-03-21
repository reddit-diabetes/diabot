package com.dongtronic.nightscout.data

import com.dongtronic.nightscout.exceptions.NoNightscoutDataException

data class NightscoutDTO(
        val entries: Set<BgEntry> = emptySet(),
        val low: Int = 0,
        val bottom: Int = 0,
        val top: Int = 0,
        val high: Int = 0,
        val iob: Float = 0.0F,
        val cob: Int = 0,
        val units: String = "",
        val title: String = "Nightscout"
) {
    /**
     * Gets the most recent BG entry.
     *
     * @return the most recent BG entry
     * @throws NoNightscoutDataException if there is no BG entry available
     */
    fun getNewestEntry(): BgEntry {
        return entries.maxBy { it.dateTime }
                ?: throw NoNightscoutDataException()
    }

    /**
     * Gets the most recent BG entry, or null if there is not one.
     *
     * @return the most recent BG entry, or null
     */
    fun getNewestEntryOrNull(): BgEntry? {
        return try {
            getNewestEntry()
        } catch (e: NoNightscoutDataException) {
            null
        }
    }

    fun newBuilder(): Builder = Builder(this)

    class Builder {
        private var entries: MutableSet<BgEntry> = mutableSetOf()
        private var low: Int = 0
        private var bottom: Int = 0
        private var top: Int = 0
        private var high: Int = 0
        private var iob: Float = 0.0F
        private var cob: Int = 0
        private var units: String = ""
        private var title: String = "Nightscout"

        constructor()

        constructor(dto: NightscoutDTO) {
            this.entries = dto.entries.toMutableSet()
            this.low = dto.low
            this.bottom = dto.bottom
            this.top = dto.top
            this.high = dto.high
            this.iob = dto.iob
            this.cob = dto.cob
            this.units = dto.units
            this.title = dto.title
        }

        fun addEntry(vararg entries: BgEntry) = apply { this.entries.addAll(entries) }

        fun removeEntry(vararg entries: BgEntry) = apply { this.entries.removeAll(entries) }

        /**
         * Adds or replaces a BG entry in the set of entries
         *
         * @param entry the BG entry to add/replace
         */
        fun replaceEntry(entry: BgEntry) = apply {
            removeEntry(entry)
            addEntry(entry)
        }

        fun clearEntries() = apply { this.entries.clear() }

        fun entries(entries: MutableSet<BgEntry>) = apply { this.entries = entries }

        fun low(low: Int) = apply { this.low = low }

        fun bottom(bottom: Int) = apply { this.bottom = bottom }

        fun top(top: Int) = apply { this.top = top }

        fun high(high: Int) = apply { this.high = high }

        fun iob(iob: Float) = apply { this.iob = iob }

        fun cob(cob: Int) = apply { this.cob = cob }

        fun units(units: String) = apply { this.units = units }

        fun title(title: String) = apply { this.title = title }

        fun build() = NightscoutDTO(
                entries = entries,
                low = low,
                bottom = bottom,
                top = top,
                high = high,
                iob = iob,
                cob = cob,
                units = units,
                title = title
        )
    }
}