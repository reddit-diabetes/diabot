package com.dongtronic.diabot.graph.theme

import java.awt.Color

class DiabotLightTheme : DiabotTheme() {
    override fun getChartBackgroundColor(): Color {
        return Color(223, 223, 223)
    }

    override fun getChartFontColor(): Color {
        return Color.BLACK
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
        return Color(0, 0, 0, 0)
    }

    override fun getPlotBorderColor(): Color {
        return plotBackgroundColor
    }

    override fun getPlotBackgroundColor(): Color {
        return Color(230, 230, 230)
    }
}