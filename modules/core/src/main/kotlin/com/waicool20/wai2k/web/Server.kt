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
import com.waicool20.cvauto.android.ADB
import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.util.YuuBot
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.waicoolutils.javafx.json.ListPropertyDeserializer
import com.waicool20.waicoolutils.javafx.json.ignoreJavaFXPropertyTypes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import javafx.beans.property.ListProperty
import kotlinx.coroutines.*
import java.awt.image.DataBufferByte
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.system.measureTimeMillis

data class CheckApiKeyRequest(val apiKey: String)
data class CheckApiMessageRequest(val apiKey: String, val title: String, val message: String)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class StatusResponse(val message: String)

class Server {
    fun run() {
        io.ktor.server.netty.EngineMain.main(emptyArray())
    }
}

val logger = loggerFor<Server>()

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
    routing {
        get("/") {
            call.respond(Store.config())
        }
        post("/") {
            val config = call.receive<Wai2kConfig>()
            logger.debug(config.currentProfile)
            call.respond(config)
        }
        get("/profiles") {
            call.respond(Store.profiles())
        }
        profile()
        classifier()
        get("/devices") {
            call.respond(ADB.getDevices().map { it.serial })
        }
        device()
        yuubot()
    }
}

private fun Routing.profile() = route("/profile") {
    get("/{name}") {
        val config = Store.config()
        config.currentProfile = call.parameters["name"]
        config.save()
        call.respond(Store.profile(call.parameters["name"]))
    }
    post("/{name}") {
        val config = Store.config()
        config.currentProfile = call.parameters["name"]
        config.save()
        val profile = call.receive<Wai2kProfile>()
        profile.also {
            it.name = call.parameters["name"]
            logger.debug(it.toString())
        }.save()
        call.respond(Store.profile(call.parameters["name"]))
    }
    delete("/{name}") {
        Store.profile(call.parameters["name"]).delete()
        call.respond(HttpStatusCode.OK)
    }
}

private fun Routing.classifier() = route("/classifier") {
    get {
        call.respond(Store.classifier())
    }
    get("/maps") {
        call.respond(Store.maps())
    }
}

private fun Routing.device() = route("/device/{serial}") {
    get("/connect") {
        val device = suspendCoroutine { cont ->
            ADB.connect(call.parameters["serial"] ?: "", cont::resume)
        }

        if (device == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(HttpStatusCode.OK)
        }
    }
    webSocket("/test-latency") {
        val device = ADB.getDevice(call.parameters["serial"] ?: "")
            ?: run {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid device serial"))
                return@webSocket
            }
        val times = call.request.queryParameters["times"]?.toIntOrNull() ?: 10
        var totalTime = 0L

        repeat(times) { i ->
            val time = measureTimeMillis { device.screens[0].capture() }
            delay(100)
            send(Frame.Text("$i $time"))
            totalTime += time
        }
        send(Frame.Text("avg ${totalTime / times}"))
        close(CloseReason(CloseReason.Codes.NORMAL, "End of Test"))
    }
    get("/toggle-pointer-info") {
        val device = ADB.getDevice(call.parameters["serial"] ?: "")
            ?: run {
                call.respond(HttpStatusCode.BadRequest, StatusResponse("Invalid device serial"))
                return@get
            }
        device.togglePointerInfo()
        call.respond(HttpStatusCode.OK)
    }
    get("/toggle-touches") {
        val device = ADB.getDevice(call.parameters["serial"] ?: "")
            ?: run {
                call.respond(HttpStatusCode.BadRequest, StatusResponse("Invalid device serial"))
                return@get
            }
        device.toggleTouches()
        call.respond(HttpStatusCode.OK)
    }
    get("/capture") {
        val device = ADB.getDevice(call.parameters["serial"] ?: "")
            ?: run {
                call.respond(HttpStatusCode.BadRequest, StatusResponse("Invalid device serial"))
                return@get
            }
        val img = device.screens[0].capture()
        when (call.request.queryParameters["format"]?.lowercase()) {
            "png" -> call.respondOutputStream(ContentType.Image.PNG, HttpStatusCode.OK) {
                withContext(Dispatchers.IO) {
                    ImageIO.write(img, "PNG", this@respondOutputStream)
                }
            }
            "jpg", "jpeg" -> call.respondOutputStream(
                ContentType.Image.JPEG,
                HttpStatusCode.OK
            ) {
                withContext(Dispatchers.IO) {
                    ImageIO.write(img, "JPEG", this@respondOutputStream)
                }
            }
            "raw", null -> call.respondOutputStream(
                ContentType.Application.OctetStream,
                HttpStatusCode.OK
            ) {
                withContext(Dispatchers.IO) {
                    write((img.raster.dataBuffer as DataBufferByte).data)
                }
            }
            else -> call.respond(
                HttpStatusCode.BadRequest,
                StatusResponse("Format must be one of [png|jpg|jpeg|raw]")
            )
        }
    }
}

private fun Routing.yuubot() = route("/yuubot") {
    post("/apikey") {
        val request = call.receive<CheckApiKeyRequest>()
        val status = suspendCoroutine { cont ->
            YuuBot(request.apiKey).testApiKey(cont::resume)
        }
        when (status) {
            YuuBot.ApiKeyStatus.VALID -> call.respond(HttpStatusCode.OK)
            YuuBot.ApiKeyStatus.INVALID -> call.respond(
                HttpStatusCode.NotAcceptable,
                StatusResponse("Invalid API key")
            )
            YuuBot.ApiKeyStatus.UNKNOWN -> call.respond(
                HttpStatusCode.NotModified,
                StatusResponse("Unknown Yuu error")
            )
        }
    }
    post("/message") {
        val request = call.receive<CheckApiMessageRequest>()
        val status = suspendCoroutine { cont ->
            YuuBot(request.apiKey).postMessage(request.title, request.message, cont::resume)
        }
        when (status) {
            YuuBot.MessageStatus.OK -> call.respond(HttpStatusCode.OK)
            YuuBot.MessageStatus.FAIL -> call.respond(HttpStatusCode.BadRequest)
        }
    }
}

