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

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.wai2k.Wai2K
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.javafx.json.fxJacksonObjectMapper
import com.waicool20.waicoolutils.logging.LoggerUtils
import com.waicool20.waicoolutils.logging.loggerFor
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
class Wai2KConfig(
    currentProfile: String = Wai2KProfile.DEFAULT_NAME,
    assetsDirectory: Path = Wai2K.CONFIG_DIR.resolve("assets"),
    ocrDirectory: Path = Wai2K.CONFIG_DIR.resolve("tessdata"),
    clearConsoleOnStart: Boolean = true,
    showConsoleOnStart: Boolean = true,
    debugModeEnabled: Boolean = true,
    lastDeviceSerial: String = "",
    scriptConfig: ScriptConfig = ScriptConfig(),
    gameRestartConfig: GameRestartConfig = GameRestartConfig(),
    apiKey: String = "",
    notificationsConfig: NotificationsConfig = NotificationsConfig()
) {
    private val logger = loggerFor<Wai2K>()

    @get:JsonIgnore val isValid get() = ocrIsValid()
    @get:JsonIgnore val logLevel get() = if (debugModeEnabled) "DEBUG" else "INFO"

    //<editor-fold desc="Properties">

    val currentProfileProperty = currentProfile.toProperty()
    val assetsDirectoryProperty = assetsDirectory.toProperty()
    val ocrDirectoryProperty = ocrDirectory.toProperty()
    val clearConsoleOnStartProperty = clearConsoleOnStart.toProperty()
    val showConsoleOnStartProperty = showConsoleOnStart.toProperty()
    val debugModeEnabledProperty = debugModeEnabled.toProperty()
    val lastDeviceSerialProperty = lastDeviceSerial.toProperty()
    val scriptConfigProperty = scriptConfig.toProperty()
    val gameRestartConfigProperty = gameRestartConfig.toProperty()
    val apiKeyProperty = apiKey.toProperty()
    val notificationsConfigProperty = notificationsConfig.toProperty()

    var currentProfile by currentProfileProperty
    @get:JsonIgnore var assetsDirectory by assetsDirectoryProperty
    @get:JsonIgnore var ocrDirectory by ocrDirectoryProperty
    var clearConsoleOnStart by clearConsoleOnStartProperty
    var showConsoleOnStart by showConsoleOnStartProperty
    var debugModeEnabled by debugModeEnabledProperty
    var lastDeviceSerial by lastDeviceSerialProperty
    var scriptConfig by scriptConfigProperty
    var gameRestartConfig by gameRestartConfigProperty
    var apiKey by apiKeyProperty
    var notificationsConfig by notificationsConfigProperty

    //</editor-fold>

    companion object Loader {
        private val mapper = fxJacksonObjectMapper()
        private val loaderLogger = loggerFor<Loader>()

        val requiredOcrFiles = listOf(
            "eng.traineddata", "osd.traineddata"
        )

        val path: Path = Wai2K.CONFIG_DIR.resolve("preferences.json")

        fun load(): Wai2KConfig {
            loaderLogger.info("Attempting to load Wai2K configuration")
            loaderLogger.debug("Config path: $path")
            if (Files.notExists(path)) {
                loaderLogger.info("Configuration not found, creating empty file")
                Files.createDirectories(path.parent)
                Files.createFile(path)
            }

            return try {
                mapper.readValue<Wai2KConfig>(path.toFile()).also {
                    loaderLogger.info("Wai2K configuration loaded")
                    it.printDebugInfo()
                    if (it.currentProfile.isBlank()) it.currentProfile = Wai2KProfile.DEFAULT_NAME
                }
            } catch (e: JsonMappingException) {
                if (e.message?.startsWith("No content to map due to end-of-input") == false) {
                    throw e
                }
                loaderLogger.info("Using default config")
                Wai2KConfig().apply { save() }
            }
        }
    }

    init {
        LoggerUtils.setLogLevel(Level.toLevel(logLevel))
        debugModeEnabledProperty.addListener("LogLevel") { _ ->
            LoggerUtils.setLogLevel(Level.toLevel(logLevel))
        }
    }

    fun ocrIsValid(): Boolean {
        return requiredOcrFiles.all { Files.exists(ocrDirectory.resolve(it)) }
    }

    fun save() {
        logger.info("Saving Wai2K configuration file")
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this)
        logger.info("Saving Wai2K configuration was successful")
        printDebugInfo()
    }

    private fun printDebugInfo() {
        logger.debug("Config path: $path")
        logger.debug("Config: $this")
    }

    override fun toString(): String = mapper.writeValueAsString(this)
}
