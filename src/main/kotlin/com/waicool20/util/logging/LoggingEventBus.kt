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

package com.waicool20.util.logging

import com.waicool20.util.streams.LineBufferedOutputStream
import com.waicool20.util.streams.TeeOutputStream
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicBoolean

object LoggingEventBus {
    private val listeners = mutableListOf<Listener>()

    private class LoggingEventBusOutputStream : LineBufferedOutputStream() {
        override fun writeLine(line: String) {
            listeners.forEach { (regex, action) ->
                regex.matchEntire(line.trim())?.let(action)
            }
        }
    }

    private val stream by lazy { LoggingEventBusOutputStream() }
    private val initialized = AtomicBoolean(false)

    data class Listener(val regex: Regex, val action: (match: MatchResult) -> Unit)

    init {
        initialize()
    }

    fun initialize() {
        if (initialized.get()) return

        System.setOut(PrintStream(TeeOutputStream(System.out, stream)))
        System.setErr(PrintStream(TeeOutputStream(System.err, stream)))

        initialized.set(true)
    }

    fun subscribe(regex: Regex, listener: (match: MatchResult) -> Unit) {
        listeners.add(Listener(regex, listener))
    }
}
