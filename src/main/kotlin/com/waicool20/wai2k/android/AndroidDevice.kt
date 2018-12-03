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
import com.waicool20.wai2k.util.executeOrShell
import com.waicool20.waicoolutils.and
import com.waicool20.waicoolutils.logging.loggerFor
import org.sikuli.script.IScreen
import se.vidstige.jadb.JadbDevice
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Represents an android device
 */
class AndroidDevice(
        private val adbServer: AdbServer,
        private val device: JadbDevice
) {

    private val logger = loggerFor<AndroidDevice>()

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

    /**
     * Backing value of the [fastScreenshotMode] property
     */
    private val _fastScreenshotMode = AtomicBoolean(false)

    /**
     * Enables fast screenshot mode
     */
    var fastScreenshotMode: Boolean
        get() = _fastScreenshotMode.get()
        set(value) {
            // Starts the process if trying to enable it
            if (value) takeFastScreenshot()
            _fastScreenshotMode.set(value)
        }

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

        fun readDeviceInfo(): String? {
            return device.executeAndReadText("getevent -p").split("add device").find { it.contains("ABS") }
        }

        val deviceInfo = readDeviceInfo() ?: run {
            repeat(5) { i ->
                logger.warn("Failed to read device info, will try to restart adb and retry")
                adbServer.restart()
                adbServer.waitForInitialized()
                readDeviceInfo()?.let {
                    logger.info("Restarted ${i + 1} times")
                    return@run it
                }
            }
            null
        } ?: error("This screen does not support touch/tap events")

        input = AndroidInput.parse(deviceInfo)
    }

    /**
     * Executes a given command on the device
     *
     * @param command The command to execute
     * @param args Arguments for the command
     *
     * @return [InputStream] of the given command
     */
    fun execute(command: String, vararg args: String): InputStream = device.executeOrShell(command, *args)

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
     * Checks if the showing pointer information
     *
     * @returns True if pointer info is on screen
     */
    fun isShowingPointerInfo(): Boolean {
        return device.executeAndReadText("settings get system pointer_location") == "1"
    }

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
        device.execute("settings put system pointer_location ${if (isShowingPointerInfo()) "0" else "1"}")
    }

    /**
     * Get Orientation of the phone
     */
    fun getOrientation(): Int {
        return device.executeAndReadText("dumpsys input | grep SurfaceOrientation")
                .takeLast(1).toIntOrNull() ?: 0
    }

    //<editor-fold desc="Screenshot">

    /**
     * Takes a screenshot of the screen of the device
     *
     * @return [BufferedImage] containing the data of the screenshot
     */
    fun takeScreenshot(): BufferedImage {
        if (fastScreenshotMode) return takeFastScreenshot()
        var exception: Exception? = null
        for (i in 0 until 3) {
            try {
                val buffer = ByteBuffer.wrap(device.executeOrShell("screencap").readBytes())
                        .order(ByteOrder.LITTLE_ENDIAN)

                val width = buffer.int
                val height = buffer.int
                val image = BufferedImage(width, height, buffer.int)
                buffer.int // Ignore the 4th int

                val imageBuffer = (image.raster.dataBuffer as DataBufferInt).data
                for (pixel in 0 until imageBuffer.size) {
                    val r = ((buffer.get() and 0xFF) shl 16)
                    val g = ((buffer.get() and 0xFF) shl 8)
                    val b = (buffer.get() and 0xFF)
                    buffer.get()
                    imageBuffer[pixel] = r or g or b
                }
                return image
            } catch (e: Exception) {
                exception = e
            }
        }
        throw exception ?: error("Could not take screenshot due to unknown error")
    }

    private val screenshotBufferSize = properties.displayWidth * properties.displayHeight * 3
    private val screenshotRenderBuffer = ByteBuffer.allocateDirect(screenshotBufferSize)
    private val screenshotReadBuffer = ByteArray(screenshotBufferSize)
    private var screenshotIsRendering = false

    private var screenRecordProcess: Process? = null
    private var lastScreenshot: BufferedImage? = null
    private var lastScreenshotTime = System.currentTimeMillis()
    private val screenshotExpiryTime = 15

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            screenRecordProcess?.destroy()
        })
    }

    /**
     * Takes a screenshot of the screen of the device using screenrecord technique which sends
     * continuous video data. The image is rendered on demand and should be faster since
     * the data is already in program memory.
     */
    private fun takeFastScreenshot(): BufferedImage {
        fun renderScreenshot(): BufferedImage {
            screenshotIsRendering = true
            val shallowBuffer = screenshotRenderBuffer.duplicate().apply { clear() }
            val width = properties.displayWidth
            val height = properties.displayHeight
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val imageBuffer = (image.raster.dataBuffer as DataBufferInt).data
            for (pixel in 0 until imageBuffer.size) {
                val r = ((shallowBuffer.get() and 0xFF) shl 16)
                val g = ((shallowBuffer.get() and 0xFF) shl 8)
                val b = (shallowBuffer.get() and 0xFF)
                imageBuffer[pixel] = r or g or b
            }
            lastScreenshot = image
            lastScreenshotTime = System.currentTimeMillis()
            screenshotIsRendering = false
            return image
        }

        if (screenRecordProcess == null || screenRecordProcess?.isAlive == false) {
            thread {
                screenRecordProcess?.destroy()
                screenRecordProcess = ProcessBuilder("adb", "shell", "screenrecord", "--output-format=raw-frames", "-").start()
                screenRecordProcess?.inputStream?.let { inputStream ->
                    var offset = 0
                    while (screenRecordProcess?.isAlive == true && fastScreenshotMode) {
                        while (offset < screenshotReadBuffer.size) {
                            offset += inputStream.read(screenshotReadBuffer, offset, screenshotReadBuffer.size - offset)
                        }
                        if (!screenshotIsRendering) {
                            screenshotRenderBuffer.clear()
                            screenshotRenderBuffer.put(screenshotReadBuffer)
                        }
                        offset = 0
                    }
                    screenRecordProcess?.destroy()
                }
            }
        }
        lastScreenshot?.takeIf {
            System.currentTimeMillis() - lastScreenshotTime < screenshotExpiryTime
        }?.let { return it }
        return renderScreenshot()
    }

    //</editor-fold>
}
