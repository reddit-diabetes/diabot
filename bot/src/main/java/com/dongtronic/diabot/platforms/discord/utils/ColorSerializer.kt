package com.dongtronic.diabot.platforms.discord.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.awt.Color

class ColorSerializer : JsonSerializer<Color>() {
    override fun serialize(value: Color, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("argb", Integer.toHexString(value.rgb))
        gen.writeEndObject()
    }
}