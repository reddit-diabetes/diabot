package com.dongtronic.diabot.graph

import com.dongtronic.diabot.graph.theme.DiabotDarkTheme
import com.dongtronic.diabot.graph.theme.DiabotLightTheme
import com.dongtronic.diabot.graph.theme.DiabotTheme

enum class GraphTheme(val instance: DiabotTheme) {
    DARK(DiabotDarkTheme()),
    LIGHT(DiabotLightTheme())
}