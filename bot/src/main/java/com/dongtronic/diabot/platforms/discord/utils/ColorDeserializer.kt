package com.dongtronic.diabot.platforms.discord.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import java.awt.Color

class ColorDeserializer : JsonDeserializer<Color>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Color {
        val node: JsonNode = p.codec.readTree(p)
        val argb = node.get("argb")
        return Color(Integer.parseUnsignedInt(argb.asText(), 16))
    }
}