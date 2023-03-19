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

package com.waicool20.wai2k.web

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.waicoolutils.javafx.json.ListPropertyDeserializer
import com.waicool20.waicoolutils.javafx.json.ignoreJavaFXPropertyTypes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import javafx.beans.property.ListProperty
import kotlinx.coroutines.*

data class CheckApiKeyRequest(val apiKey: String)
data class CheckApiMessageRequest(val apiKey: String, val title: String, val message: String)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class StatusResponse(val message: String)

class Server {
    fun run() {
        io.ktor.server.netty.EngineMain.main(emptyArray())
    }
}

fun Application.module() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        allowNonSimpleContentTypes = true
        anyHost()
    }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())

            // @TODO: gito: get rid of it when we remove JavaFX deps
            registerModule(SimpleModule().apply {
                addDeserializer(ListProperty::class.java, ListPropertyDeserializer<Any>())
            })
            ignoreJavaFXPropertyTypes()
        }
    }
    install(WebSockets)
}
