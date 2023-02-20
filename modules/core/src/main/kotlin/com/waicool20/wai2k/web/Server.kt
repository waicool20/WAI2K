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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.waicool20.cvauto.android.ADB
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.util.YuuBot
import com.waicool20.wai2k.util.loggerFor
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch

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
        }
    }
    routing {
        get("/") {
            call.respond(Store.config())
        }
        get("/profiles") {
            call.respond(Store.profiles())
        }
        get("/profile/current") {
            call.respond(Store.profile())
        }
        get("/profile/{name}") {
            val logger = loggerFor<Application>();
            logger.debug(call.parameters.toString())
            call.respond(Store.profile(call.parameters["name"]))
        }
        delete("/profile/{name}") {
            Store.profile(call.parameters["name"]).delete()
            call.respond(Pair(StatusResponse("Deleted"), HttpStatusCode.Accepted))
        }
        get("/classifier/maps") {
            call.respond(Store.maps())
        }
        get("/classifier") {
            call.respond(Store.classifier())
        }
        get("/device/list") {
            call.respond(ADB.getDevices())
        }
        post("/yuubot/apikey") {
            val request = call.receive<CheckApiKeyRequest>()
            var response = Pair(StatusResponse("Unknown Yuu error"), HttpStatusCode.NotModified)
            val countDownLatch = CountDownLatch(1)

            YuuBot(request.apiKey).testApiKey { status ->
                response = when (status) {
                    YuuBot.ApiKeyStatus.VALID -> {
                        Pair(StatusResponse("Success"), HttpStatusCode.OK)
                    }
                    YuuBot.ApiKeyStatus.INVALID -> {
                        Pair(StatusResponse("Invalid API key"), HttpStatusCode.NotAcceptable)
                    }
                    YuuBot.ApiKeyStatus.UNKNOWN -> {
                        Pair(StatusResponse("Unknown Yuu error"), HttpStatusCode.NotModified)
                    }
                }
                countDownLatch.countDown()
            }

            withContext(Dispatchers.IO) {
                countDownLatch.await()
                call.respond(response.second, response.first)
            }
        }
        post("/yuubot/message") {
            val request = call.receive<CheckApiMessageRequest>()
            val response = Pair(StatusResponse("Sent message"), HttpStatusCode.OK)
            val countDownLatch = CountDownLatch(1)

            YuuBot(request.apiKey).postMessage(request.title, request.message) {
                countDownLatch.countDown()
            }

            withContext(Dispatchers.IO) {
                countDownLatch.await()
                call.respond(response.second, response.first)
            }
        }
    }
}
