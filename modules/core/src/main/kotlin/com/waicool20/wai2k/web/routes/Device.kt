/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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

package com.waicool20.wai2k.web.routes

import com.waicool20.cvauto.android.ADB
import com.waicool20.wai2k.web.StatusResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.image.DataBufferByte
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.system.measureTimeMillis

fun Application.deviceRoutes() {
    routing {
        get("/devices") {
            call.respond(ADB.getDevices().map { it.serial })
        }
        route("/device/{serial}") {
            connect()
            testLatency()
            togglePointerInfo()
            toggleTouches()
            getCapture()
        }
    }
}

private fun Route.getCapture() {
    get("/capture") {
        val device = ADB.getDevice(call.parameters["serial"] ?: "")
            ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    StatusResponse("Invalid device serial")
                )
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

private fun Route.toggleTouches() {
    get("/toggle-touches") {
        val device = ADB.getDevice(call.parameters["serial"] ?: "")
            ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    StatusResponse("Invalid device serial")
                )
                return@get
            }
        device.toggleTouches()
        call.respond(HttpStatusCode.OK)
    }
}

private fun Route.togglePointerInfo() {
    get("/toggle-pointer-info") {
        val device = ADB.getDevice(call.parameters["serial"] ?: "")
            ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    StatusResponse("Invalid device serial")
                )
                return@get
            }
        device.togglePointerInfo()
        call.respond(HttpStatusCode.OK)
    }
}

private fun Route.testLatency() {
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
}

private fun Route.connect() {
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
}