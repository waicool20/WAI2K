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
import java.util.concurrent.TimeUnit

class AdbServer {

    private val logger = loggerFor<AdbServer>()

    fun start() {
        if (!isRunning()) {
            logger.info("ADB Server not running, launching new instance")
            ProcessBuilder(resolveAdb(), "start-server").start()
            logger.info("ADB Server launched")
        }
    }

    fun waitForInitialized() {
        while (!isRunning()) TimeUnit.MILLISECONDS.sleep(100)
    }

    fun restart() {
        logger.info("Restarting ADB Server")
        stop()
        start()
    }

    fun stop() {
        if (isRunning()) {
            logger.info("ADB Server running, killing it")
            ProcessBuilder(resolveAdb(), "kill-server").start().waitFor()
            logger.info("ADB Server Killed")
        }
    }

    fun isRunning(): Boolean {
        ProcessBuilder(resolveAdb(), "devices").start().apply {
            return inputStream.bufferedReader().readText().isNotEmpty()
        }
        return false
    }

    fun resolveAdb(): String {
        val home = System.getenv("ANDROID_HOME")
                ?: System.getenv("ANDROID_SDK_ROOT") ?: return "adb"
        return "$home/platform-tools/adb".takeIf { home.isNotBlank() } ?: "adb"
    }
}