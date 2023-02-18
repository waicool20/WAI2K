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

package com.waicool20.wai2k.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.wai2k.Wai2k
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.javafx.json.fxJacksonObjectMapper
import com.waicool20.wai2k.util.loggerFor
import tornadofx.*
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.notExists

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
class Wai2kConfig(
    currentProfile: String = Wai2kProfile.DEFAULT_NAME,
    assetsDirectory: Path = Wai2k.CONFIG_DIR.resolve("assets"),
    clearConsoleOnStart: Boolean = true,
    showConsoleOnStart: Boolean = true,
    captureMethod: AndroidDevice.CaptureMethod = AndroidDevice.CaptureMethod.SCRCPY,
    captureCompressionMode: AndroidDevice.CompressionMode = AndroidDevice.CompressionMode.LZ4,
    lastDeviceSerial: String = "",
    scriptConfig: ScriptConfig = ScriptConfig(),
    gameRestartConfig: GameRestartConfig = GameRestartConfig(),
    apiKey: String = "",
    notificationsConfig: NotificationsConfig = NotificationsConfig(),
    appearanceConfig: AppearanceConfig = AppearanceConfig()
) {
    private val logger = loggerFor<Wai2kConfig>()

    //<editor-fold desc="Properties">

    val currentProfileProperty = currentProfile.toProperty()
    val assetsDirectoryProperty = assetsDirectory.toProperty()
    val clearConsoleOnStartProperty = clearConsoleOnStart.toProperty()
    val showConsoleOnStartProperty = showConsoleOnStart.toProperty()
    val captureMethodProperty = captureMethod.toProperty()
    val captureCompressionModeProperty = captureCompressionMode.toProperty()
    val lastDeviceSerialProperty = lastDeviceSerial.toProperty()
    val scriptConfigProperty = scriptConfig.toProperty()
    val gameRestartConfigProperty = gameRestartConfig.toProperty()
    val apiKeyProperty = apiKey.toProperty()
    val notificationsConfigProperty = notificationsConfig.toProperty()
    val appearanceConfigProperty = appearanceConfig.toProperty()

    var currentProfile by currentProfileProperty
    @get:JsonIgnore var assetsDirectory by assetsDirectoryProperty
    var clearConsoleOnStart by clearConsoleOnStartProperty
    var showConsoleOnStart by showConsoleOnStartProperty
    var captureMethod by captureMethodProperty
    var captureCompressionMode by captureCompressionModeProperty
    var lastDeviceSerial by lastDeviceSerialProperty
    var scriptConfig by scriptConfigProperty
    var gameRestartConfig by gameRestartConfigProperty
    var apiKey by apiKeyProperty
    var notificationsConfig by notificationsConfigProperty
    var appearanceConfig by appearanceConfigProperty

    //</editor-fold>

    companion object Loader {
        private val mapper = fxJacksonObjectMapper()
        private val loaderLogger = loggerFor<Loader>()

        val path: Path = Wai2k.CONFIG_DIR.resolve("preferences.json")

        fun load(): Wai2kConfig {
            loaderLogger.info("Attempting to load WAI2K configuration")
            loaderLogger.debug("Config path: $path")
            if (path.notExists()) {
                loaderLogger.info("Configuration not found, creating empty file")
                path.parent.createDirectories()
                path.createFile()
            }

            return try {
                mapper.readValue<Wai2kConfig>(path.toFile()).also {
                    loaderLogger.info("WAI2K configuration loaded")
                    it.printDebugInfo()
                    if (it.currentProfile.isBlank()) it.currentProfile = Wai2kProfile.DEFAULT_NAME
                }
            } catch (e: JsonMappingException) {
                if (e.message?.startsWith("No content to map due to end-of-input") == false) {
                    throw e
                }
                loaderLogger.info("Using default config")
                Wai2kConfig().apply { save() }
            }
        }
    }

    init {
        captureMethodProperty.addListener("CMListener") { newVal ->
            ADB.getDevice(lastDeviceSerial)?.captureMethod = newVal
        }
        captureCompressionModeProperty.addListener("CCMListener") { newVal ->
            ADB.getDevice(lastDeviceSerial)?.compressionMode = newVal
        }
    }

    @JsonIgnore
    fun setNewDevice(device: AndroidDevice) {
        lastDeviceSerial = device.serial
        device.captureMethod = captureMethod
        device.compressionMode = captureCompressionMode
    }

    fun save() {
        logger.info("Saving WAI2K configuration file")
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this)
        logger.info("Saving WAI2K configuration was successful")
        printDebugInfo()
    }

    private fun printDebugInfo() {
        logger.debug("Config path: $path")
        logger.debug("Config: $this")
    }

    override fun toString(): String = mapper.writeValueAsString(this)
}
