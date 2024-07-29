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

package com.waicool20.wai2k.game

import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.wai2k.events.DollDropEvent
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

object GFL {
    const val PKG_NAME = "com.sunborn.girlsfrontline.en"

    const val MAX_ECHELON = 14

    class LogcatListener(val scriptRunner: ScriptRunner, val device: AndroidDevice) {
        private val logger = loggerFor<LogcatListener>()
        private val currentThread = AtomicReference<Thread?>(null)
        private var currentProcess: Process? = null
        private val r =
            Regex("(\\d\\d-\\d\\d) (\\d\\d:\\d\\d:\\d\\d.\\d{3})\\s+(\\d+)\\s+(\\d+)\\s+([VDIWEFS])\\s+(\\w+)\\s+: (.*)\$")

        fun start() {
            thread(isDaemon = true, name = "Logcat Listener [${device.serial}]") {
                while (!Thread.interrupted()) {
                    if (!device.isConnected()) {
                        TimeUnit.SECONDS.sleep(5)
                        continue
                    }
                    ADB.execute("-s", device.serial, "logcat", "-c") // Clear logs
                    val process = ADB.execute("-s", device.serial, "logcat", "Unity:V", "*:S")
                    try {
                        process.inputStream.bufferedReader().forEachLine(::onNewLine)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                currentProcess?.destroyForcibly()
                currentThread.set(null)
            }.also { currentThread.set(it) }
        }

        fun stop() {
            currentThread.get()?.interrupt()
            currentProcess?.destroyForcibly()
        }

        private val _lines = MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 2048,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val lines = _lines.asSharedFlow()

        private val gunCheckRegex = Regex(".*加载热更资源包名.+character(.+)\\.ab.*")
        private var gotGun = false

        private fun onNewLine(l: String) {
            when {
                !gotGun && l.contains("预制物GetNewGun") -> gotGun = true
                gotGun -> checkForGun(l)
            }
            scriptRunner.sessionScope.launch {
                val match = r.matchEntire(l) ?: return@launch
                // date, time, pid, tid, level, tag, msg
                val (_, _, _, _, _, _, msg) = match.destructured
                _lines.emit(msg)
            }
        }

        private fun checkForGun(l: String) {
            var name = gunCheckRegex.matchEntire(l)?.groupValues?.get(1) ?: return
            name = name.removeSuffix("he")
            name = name.removeSuffix("nom")
            if (scriptRunner.state == ScriptRunner.State.RUNNING && scriptRunner.profile.combat.enabled) {
                EventBus.tryPublish(
                    DollDropEvent(
                        name,
                        scriptRunner.profile.combat.map,
                        scriptRunner.sessionId,
                        scriptRunner.elapsedTime
                    )
                )
            }
            logger.info("Got new gun: $name")
            gotGun = false
        }
    }
}
