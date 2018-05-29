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
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

class AndroidDevice private constructor(private val device: JadbDevice) {
    data class Properties(
            val androidVersion: String,
            val brand: String,
            val manufacturer: String,
            val model: String,
            val name: String,
            val displayWidth: Int,
            val displayHeight: Int
    )

    val properties: Properties
    val adbSerial: String = device.serial
    val input: AndroidInput
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
        private val adb by lazy { JadbConnection() }
        fun listAll() = adb.devices.map { AndroidDevice(it) }
    }

    fun execute(command: String, vararg args: String): InputStream = device.execute(command, *args)
    fun executeAndReadLines(command: String, vararg args: String) = device.executeAndReadLines(command, *args)
    fun executeAndReadText(command: String, vararg args: String) = device.executeAndReadText(command, *args)

    fun displayPointerInfo(display: Boolean) {
        device.execute("settings put system pointer_location ${if (display) "1" else "0"}")
    }

    fun togglePointerInfo() {
        val isShowing = device.executeAndReadText("settings get system pointer_location") == "1"
        device.execute("settings put system pointer_location ${if (isShowing) "0" else "1"}")
    }

    fun takeScreenshot(): BufferedImage {
        return ImageIO.read(device.execute("screencap -p"))
    }
}
