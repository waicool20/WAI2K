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

import com.waicool20.wai2k.util.YuuBot
import com.waicool20.wai2k.web.StatusResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class CheckApiKeyRequest(val apiKey: String)
data class CheckApiMessageRequest(val apiKey: String, val title: String, val message: String)

fun Application.yuubotRoutes() {
    routing {
        route("/yuubot") {
            apiKey()
            message()
        }
    }
}

private fun Route.message() {
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

private fun Route.apiKey() {
    post("/apikey") {
        val request = call.receive<CheckApiKeyRequest>()
        val status = suspendCoroutine { cont ->
            YuuBot(request.apiKey).testApiKey(cont::resume)
        }
        when (status) {
            YuuBot.ApiKeyStatus.VALID -> call.respond(HttpStatusCode.OK)
            YuuBot.ApiKeyStatus.INVALID -> call.respond(
                HttpStatusCode.NotAcceptable, StatusResponse("Invalid API key")
            )
            YuuBot.ApiKeyStatus.UNKNOWN -> call.respond(
                HttpStatusCode.NotModified, StatusResponse("Unknown Yuu error")
            )
        }
    }
}