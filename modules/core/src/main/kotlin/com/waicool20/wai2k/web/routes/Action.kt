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

import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.wai2k.web.Store
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import tornadofx.*

data class RunnerStatus(val status: String) {}

val logger = loggerFor<Wai2k>()

fun Application.actionRoutes() {
    routing {
        route("/actions") {
            statusRunner()
            startRunner()
            pauseRunner()
            stopRunner()
        }
    }
}


private fun Route.statusRunner() {
    get("/status") {
        val scriptRunner = Store.runner()

        when (scriptRunner.state) {
            ScriptRunner.State.RUNNING -> {
                scriptRunner.pause()
                logger.info("Script will pause when the current cycle ends")
            }
            ScriptRunner.State.PAUSING, ScriptRunner.State.PAUSED -> {
                scriptRunner.unpause()
            }
            ScriptRunner.State.STOPPED -> {
                scriptRunner.apply {
                    config = Wai2k.config
                    profile = Wai2k.profile
                }.run()
            }
        }

        call.respond(HttpStatusCode.OK)
    }
}

private fun Route.startRunner() {
    get("/start") {
        val scriptRunner = Store.runner()

        if (scriptRunner.state == ScriptRunner.State.STOPPED) {
            scriptRunner.apply {
                config = Wai2k.config
                profile = Wai2k.profile
            }.run()
        }

        if (scriptRunner.state == ScriptRunner.State.PAUSING || scriptRunner.state == ScriptRunner.State.PAUSED) {
            scriptRunner.unpause()
        }

        if (scriptRunner.state == ScriptRunner.State.RUNNING) {
            logger.info("Script already started")
        }

        call.respond(HttpStatusCode.OK)
    }
}

private fun Route.pauseRunner() {
    get("/pause") {
        val scriptRunner = Store.runner()

        if (scriptRunner.state == ScriptRunner.State.RUNNING) {
            scriptRunner.pause()
            logger.info("Script will pause when the current cycle ends")
        }

        if (scriptRunner.state == ScriptRunner.State.PAUSING) {
            logger.info("Script will pause when the current cycle ends")
        }

        if (scriptRunner.state == ScriptRunner.State.PAUSED) {
            logger.info("Script already paused")
        }

        if (scriptRunner.state == ScriptRunner.State.STOPPED) {
            logger.info("Script was stopped already")
        }

        call.respond(HttpStatusCode.OK)
    }
}

private fun Route.stopRunner() {
    get("/stop") {
        val scriptRunner = Store.runner()

        if (scriptRunner.state != ScriptRunner.State.STOPPED) {
            scriptRunner.stop()
        } else {
            logger.info("Script already stopped")
        }

        call.respond(HttpStatusCode.OK)
    }
}