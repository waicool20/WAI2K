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

package com.waicool20.wai2k.android

import com.waicool20.waicoolutils.logging.loggerFor
import se.vidstige.jadb.JadbConnection
import java.util.concurrent.TimeUnit

class AdbServer(val adbPath: String = resolveAdb()) {

    private val logger = loggerFor<AdbServer>()
    private var deviceCache: List<AndroidDevice> = emptyList()
    private val adb
        get() = newConnection()

    companion object {
        fun resolveAdb(): String {
            val home = System.getenv("ANDROID_HOME")
                    ?: System.getenv("ANDROID_SDK_ROOT") ?: return "adb"
            return "$home/platform-tools/adb".takeIf { home.isNotBlank() } ?: "adb"
        }
    }

    fun start() {
        if (!isRunning()) {
            logger.info("ADB Server not running, launching new instance")
            ProcessBuilder(adbPath, "start-server").start()
            logger.info("ADB Server launched")
        }
    }

    fun waitForInitialized() {
        while (!isRunning()) TimeUnit.MILLISECONDS.sleep(100)
    }

    fun restart() {
        logger.info("Restarting ADB Server")
        stop()
        TimeUnit.MILLISECONDS.sleep(300)
        start()
    }

    fun stop() {
        if (isRunning()) {
            logger.info("ADB Server running, killing it")
            ProcessBuilder(adbPath, "kill-server").start().waitFor()
            logger.info("ADB Server Killed")
        }
    }

    fun isRunning(): Boolean {
        ProcessBuilder(adbPath, "devices").start().apply {
            return inputStream.bufferedReader().readText().isNotEmpty()
        }
        return false
    }

    fun listDevices(refresh: Boolean = true): List<AndroidDevice> {
        return if (refresh) {
            adb.devices.map { AndroidDevice(this, it) }.also { deviceCache = it }
        } else {
            deviceCache
        }
    }

    private fun newConnection(): JadbConnection {
        start()
        waitForInitialized()
        return JadbConnection()
    }
}