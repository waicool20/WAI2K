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
import se.vidstige.jadb.JadbException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class AdbServer(path: String = resolveEnvAdb()) {
    constructor(path: Path) : this("$path")

    private val logger = loggerFor<AdbServer>()
    private var deviceCache: List<AndroidDevice> = emptyList()
    private var process: Process? = null
    private val adb
        get() = newConnection()

    private val adbPath = findAdb() ?: path

    companion object {
        /**
         * Tries to find the adb executable using the given path or by resolving through
         * environment
         *
         * @return null if a valid adb executable was not found
         */
        fun findAdb(path: String = ""): String? {
            return resolveEnvAdb().takeIf { validateAdb(it) } ?: path.takeIf { validateAdb(it) }
        }

        /**
         * Validates Adb executable
         *
         * @return true if Adb is ok
         */
        private fun validateAdb(sPath: String): Boolean {
            val path = Paths.get(sPath)
            return sPath.isNotBlank() &&
                    Files.exists(path) &&
                    Files.isRegularFile(path) &&
                    try {
                        ProcessBuilder(sPath).start().inputStream
                                .bufferedReader().readText().startsWith("Android Debug Bridge")
                    } catch (e: IOException) {
                        false
                    }
        }

        /**
         * Tries to resolve the path to default adb installation by checking path environment
         */
        private fun resolveEnvAdb(): String {
            val home = System.getenv("ANDROID_HOME")
                    ?: System.getenv("ANDROID_SDK_ROOT") ?: return "adb"
            return "$home/platform-tools/adb".takeIf { home.isNotBlank() } ?: "adb"
        }
    }

    /**
     * Starts the adb server if its not running already
     */
    fun start() {
        do {
            logger.info("Launching new adb server instance (Path: $adbPath)")
            ProcessBuilder(adbPath, "start-server").start()
            waitForInitialized()
            logger.info("ADB Server launched")
            // Launch a blocking adb operation that stays alive with the adb server
            process = ProcessBuilder(
                    adbPath,
                    "-s", "some-unlikely-device-id-123",
                    "wait-for-any-recovery"
            ).start()
            TimeUnit.SECONDS.sleep(1)
        } while (!isRunning())
    }

    /**
     * Waits until the adb server is able to receive commands
     */
    fun waitForInitialized() {
        while (
                !ProcessBuilder(adbPath, "devices").start().inputStream
                        .bufferedReader().readText().contains("List")
        ) TimeUnit.MILLISECONDS.sleep(100)
        TimeUnit.SECONDS.sleep(1)
    }

    /**
     * Restarts the adb server
     */
    fun restart() {
        logger.info("Restarting ADB Server")
        stop()
        TimeUnit.MILLISECONDS.sleep(300)
        start()
    }

    /**
     * Stops the adb server
     */
    fun stop() {
        if (isRunning()) {
            process?.destroy()
            logger.info("ADB Server running, killing it")
            ProcessBuilder(adbPath, "kill-server").start().waitFor()
            logger.info("ADB Server Killed")
        }
    }

    /**
     * Checks if the adb server is running
     */
    fun isRunning() = process?.isAlive == true

    /**
     * Lists all [AndroidDevice] connected to the server
     */
    fun listDevices(refresh: Boolean = true): List<AndroidDevice> {
        return if (refresh) {
            adb.devices.mapNotNull { newDevice ->
                try {
                    deviceCache.find { it.adbSerial == newDevice.serial }
                            ?: AndroidDevice(this, newDevice)
                } catch (e: JadbException) {
                    logger.warn("Failed to read one device: ${e.localizedMessage}")
                    null
                }
            }.also { deviceCache = it }
        } else {
            deviceCache
        }
    }

    fun execute(androidDevice: AndroidDevice, vararg args: String): ProcessBuilder {
        return ProcessBuilder(adbPath, "-s", androidDevice.adbSerial, "shell", *args)
    }

    private fun newConnection(): JadbConnection {
        if (!isRunning()) start()
        waitForInitialized()
        return JadbConnection()
    }
}