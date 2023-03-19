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

import com.waicool20.wai2k.util.IOHook
import com.waicool20.waicoolutils.streams.LineBufferedOutputStream
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.send
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking

fun Application.rootRoutes() {
    routing {
        webSocket("/io") {
            val channel = Channel<String>()
            val output = object : LineBufferedOutputStream() {
                override fun writeLine(line: String) {
                    channel.trySendBlocking(line)
                }
            }
            IOHook.hookToStdOut(output)
            IOHook.hookToStdErr(output)
            for (str in channel) {
                send(str)
            }
        }
    }
}
