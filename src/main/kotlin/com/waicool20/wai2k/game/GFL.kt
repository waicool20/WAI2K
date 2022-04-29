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

package com.waicool20.wai2k.game

import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.wai2k.events.DollDropEvent
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.waicoolutils.logging.loggerFor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object GFL {
    const val PKG_NAME = "com.sunborn.girlsfrontline.en"

    const val MAX_ECHELON = 14

    class LogcatListener(val scriptRunner: ScriptRunner, val device: AndroidDevice) {
        private val logger = loggerFor<LogcatListener>()
        private val isRunning = AtomicBoolean(false)
        private var process: Process? = null
        fun start() {
            if (!isRunning.compareAndSet(false, true)) return
            thread(isDaemon = true, name = "Logcat Listener [${device.serial}]") {
                while (isRunning.get()) {
                    if (process == null || process?.isAlive == false) {
                        if (device.isConnected()) {
                            ADB.execute("-s", device.serial, "logcat", "-c") // Clear logs
                            process = ADB.execute("-s", device.serial, "logcat", "Unity:V", "*:S")
                        } else {
                            TimeUnit.SECONDS.sleep(5)
                        }
                    }
                    try {
                        process?.inputStream?.bufferedReader()?.use { reader ->
                            reader.forEachLine {
                                if (!isRunning.get()) process?.destroy()
                                onNewLine(it)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                isRunning.set(false)
            }
        }

        fun stop() {
            isRunning.compareAndSet(true, false)
        }

        private val gunCheckRegex = Regex(".*加载热更资源包名.+character(.+)\\.ab.*")
        private var gotGun = false

        private fun onNewLine(l: String) {
            when {
                !gotGun && l.contains("预制物GetNewGun") -> gotGun = true
                gotGun -> checkForGun(l)
            }
        }

        private fun checkForGun(l: String) {
            var name = gunCheckRegex.matchEntire(l)?.groupValues?.get(1) ?: return
            name = name.removeSuffix("he")
            name = name.removeSuffix("nom")
            EventBus.tryPublish(
                DollDropEvent(
                    name,
                    scriptRunner.profile.combat.map,
                    scriptRunner.sessionId,
                    scriptRunner.elapsedTime
                )
            )
            logger.info("Got new gun: $name")
            gotGun = false
        }
    }
}
