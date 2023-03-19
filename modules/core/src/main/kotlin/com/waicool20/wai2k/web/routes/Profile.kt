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

import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.wai2k.web.Server
import com.waicool20.wai2k.web.Store
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.profileRoutes() {
    routing {
        get("/profiles") {
            call.respond(Store.profiles())
        }
        route("/profile") {
            getProfile()
            postProfile()
            deleteProfile()
        }
    }
}

private fun Route.getProfile() {
    get("/{name}") {
        val config = Store.config()
        config.currentProfile = call.parameters["name"]
        config.save()
        call.respond(Store.profile(call.parameters["name"]))
    }
}

private fun Route.postProfile() {
    post("/{name}") {
        val config = Store.config()
        config.currentProfile = call.parameters["name"]
        config.save()
        val profile = call.receive<Wai2kProfile>()
        profile.also {
            it.name = call.parameters["name"]
        }.save()
        call.respond(Store.profile(call.parameters["name"]))
    }
}

private fun Route.deleteProfile() {
    delete("/{name}") {
        Store.profile(call.parameters["name"]).delete()
        call.respond(HttpStatusCode.OK)
    }
}
