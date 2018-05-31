/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by waicool20
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.waicool20.util.javafx

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.PropertyWriter
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import javafx.beans.property.*

/**
 * Provides an object mapper which ignores JavaFX Property types
 */
fun fxJacksonObjectMapper() = jacksonObjectMapper().ignoreJavaFXPropertyTypes()

/**
 * Sets the object mapper to ignore JavaFX Property types
 */
fun ObjectMapper.ignoreJavaFXPropertyTypes() = apply {
    addMixIn(Any::class.java, FXPropertyFilter.MixIn::class.java)
    setFilterProvider(SimpleFilterProvider().addFilter("FXPropertyFilter", FXPropertyFilter()))
}

private class FXPropertyFilter : SimpleBeanPropertyFilter() {
    @JsonFilter("FXPropertyFilter")
    class MixIn

    private val filteredTypes = listOf(
            BooleanProperty::class.java,
            FloatProperty::class.java,
            DoubleProperty::class.java,
            IntegerProperty::class.java,
            LongProperty::class.java,
            StringProperty::class.java,
            ListProperty::class.java,
            SetProperty::class.java,
            MapProperty::class.java,
            ObjectProperty::class.java
    )

    override fun serializeAsField(pojo: Any, jgen: JsonGenerator, provider: SerializerProvider, writer: PropertyWriter) {
        if (include(writer)) {
            if (filteredTypes.none { writer.type.isTypeOrSubTypeOf(it) }) {
                writer.serializeAsField(pojo, jgen, provider)
            }
        } else if (!jgen.canOmitFields()) { // since 2.3
            writer.serializeAsOmittedField(pojo, jgen, provider)
        }
    }
}
