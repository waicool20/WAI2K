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

import com.waicool20.wai2k.android.input.AndroidInput
import com.waicool20.wai2k.util.executeAndReadLines
import com.waicool20.wai2k.util.executeAndReadText
import org.sikuli.script.IScreen
import se.vidstige.jadb.AdbServerLauncher
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice
import se.vidstige.jadb.Subprocess
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

/**
 * Represents an android device
 */
class AndroidDevice private constructor(private val device: JadbDevice) {
    /**
     * Wrapper class containing the basic properties of an android device
     */
    data class Properties(
            val androidVersion: String,
            val brand: String,
            val manufacturer: String,
            val model: String,
            val name: String,
            val displayWidth: Int,
            val displayHeight: Int
    )

    /**
     * Properties of this android device
     */
    val properties: Properties

    /**
     * ADB Serial no. of this android device
     */
    val adbSerial: String = device.serial

    /**
     * Input information of this android device
     */
    val input: AndroidInput

    /**
     * Gets the [IScreen] of this android device
     */
    val screen by lazy { AndroidScreen(this) }

    init {
        val props = device.executeAndReadLines("getprop").mapNotNull {
            Regex("\\[(.*?)]: \\[(.*?)]").matchEntire(it)?.groupValues?.let { it[1] to it[2] }
        }.toMap()
        val displaySize = device.executeAndReadText("wm size").let {
            Regex("Physical size: (\\d+?)x(\\d+?)").matchEntire(it)?.groupValues
                    ?: emptyList()
        }.mapNotNull { it.toIntOrNull() }

        properties = Properties(
                androidVersion = props["ro.build.version.release"] ?: "Unknown",
                brand = props["ro.product.brand"] ?: "Unknown",
                model = props["ro.product.model"] ?: "Unknown",
                manufacturer = props["ro.product.manufacturer"] ?: "Unknown",
                name = props["ro.product.name"] ?: "Unknown",
                displayWidth = displaySize[0],
                displayHeight = displaySize[1]
        )

        val deviceInfo = device.executeAndReadText("getevent -p").split("add device")
                .find { it.contains("ABS") }
                ?: error("This screen does not support touch/tap events")
        input = AndroidInput.parse(deviceInfo)
    }

    companion object {
        private val adb by lazy {
            AdbServerLauncher(Subprocess(), emptyMap()).launch()
            JadbConnection()
        }
        private var deviceCache: List<AndroidDevice> = emptyList()

        fun listAll(refresh: Boolean = true): List<AndroidDevice> {
            return if (refresh) {
                adb.devices.map { AndroidDevice(it) }.also { deviceCache = it }
            } else {
                deviceCache
            }
        }
    }

    /**
     * Executes a given command on the device
     *
     * @param command The command to execute
     * @param args Arguments for the command
     *
     * @return [InputStream] of the given command
     */
    fun execute(command: String, vararg args: String): InputStream = device.execute(command, *args)

    /**
     * Executes a given command on the device
     *
     * @param command The command to execute
     * @param args Arguments for the command
     *
     * @return List of String containing each line output of the command
     */
    fun executeAndReadLines(command: String, vararg args: String) = device.executeAndReadLines(command, *args)

    /**
     * Executes a given command on the device
     *
     * @param command The command to execute
     * @param args Arguments for the command
     *
     * @return String containing output of the command
     */
    fun executeAndReadText(command: String, vararg args: String) = device.executeAndReadText(command, *args)

    /**
     * Controls whether or not to show the pointer info on screen
     *
     * @param display Displays pointer info if True
     */
    fun displayPointerInfo(display: Boolean) {
        device.execute("settings put system pointer_location ${if (display) "1" else "0"}")
    }

    /**
     * Toggles pointer info
     */
    fun togglePointerInfo() {
        val isShowing = device.executeAndReadText("settings get system pointer_location") == "1"
        device.execute("settings put system pointer_location ${if (isShowing) "0" else "1"}")
    }

    /**
     * Takes a screenshot of the screen of the device
     *
     * @return [BufferedImage] containing the data of the screenshot
     */
    fun takeScreenshot(): BufferedImage {
        return ImageIO.read(device.execute("screencap -p"))
    }
}
