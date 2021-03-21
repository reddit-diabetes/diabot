package com.dongtronic.diabot.graph.theme

import java.awt.Color

class DiabotDarkTheme : DiabotTheme() {
    override fun getChartBackgroundColor(): Color {
        return Color(32, 32, 32)
    }

    override fun getChartFontColor(): Color {
        return Color.WHITE
    }

    override fun getAxisTickLabelsColor(): Color {
        return chartFontColor
    }

    override fun getPlotGridLinesColor(): Color {
        return chartBackgroundColor
    }

    override fun isAxisTicksMarksVisible(): Boolean {
        return false
    }

    override fun getAxisTickMarksColor(): Color {
        return Color(255, 0, 255, 0)
    }

    override fun getPlotBorderColor(): Color {
        return plotBackgroundColor
    }

    override fun getPlotBackgroundColor(): Color {
        return Color(25, 25, 25)
    }
}