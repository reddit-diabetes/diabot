package com.dongtronic.diabot.graph

import org.knowm.xchart.XYSeries

enum class PlottingStyle(val renderStyle: XYSeries.XYSeriesRenderStyle) {
    LINE(XYSeries.XYSeriesRenderStyle.Line),
    SCATTER(XYSeries.XYSeriesRenderStyle.Scatter)
}