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

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.module() {
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
            call.respond(Store.profile(call.parameters["name"]))
        }
        get("/classifier/assignment") {
            call.respond(Store.assignmentList)
        }
        get("/classifier/combat-report") {
            call.respond(Store.combatReportTypeList)
        }
        get("/classifier/logisticts-retrieval") {
            call.respond(Store.logisticsReceivalModeList)
        }
        get("/classifier/maps") {
            call.respond(Store.maps())
        }
    }
}
