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

package com.waicool20.util.streams

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class StreamGobbler(val process: Process?, autorun: Boolean = true) {
    private val latch = CountDownLatch(2)

    init {
        if (autorun) run()
    }

    fun run() {
        val handler = Thread.UncaughtExceptionHandler { _, throwable ->
            if (throwable.message == "Stream closed") {
                latch.countDown()
            } else throw throwable
        }
        process?.apply {
            thread(isDaemon = true) {
                BufferedReader(InputStreamReader(inputStream)).forEachLine(::println)
                latch.countDown()
            }.uncaughtExceptionHandler = handler
            thread(isDaemon = true) {
                BufferedReader(InputStreamReader(errorStream)).forEachLine(::println)
                latch.countDown()
            }.uncaughtExceptionHandler = handler
        }
    }

    fun waitFor() = latch.await()
}

fun Process.gobbleStream() = this.let { StreamGobbler(this, true) }
