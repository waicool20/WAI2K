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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.waicool20.util.javafx.addListener
import com.waicool20.util.javafx.fxJacksonObjectMapper
import com.waicool20.util.javafx.ignoreJavaFXPropertyTypes
import com.waicool20.util.logging.LoggerUtils
import com.waicool20.util.logging.loggerFor
import com.waicool20.wai2k.Wai2K
import com.waicool20.wai2k.android.AndroidDevice
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
class Wai2KConfig(
        currentProfile: String = "",
        sikulixJarPath: Path = Paths.get(""),
        assetsDirectory: Path = Wai2K.CONFIG_DIR.resolve("assets"),
        clearConsoleOnStart: Boolean = true,
        debugModeEnabled: Boolean = true,
        lastDeviceSerial: String = ""
) {
    private val logger = loggerFor<Wai2K>()

    @get:JsonIgnore val isValid get() = sikulixJarIsValid()
    @get:JsonIgnore val logLevel get() = if (debugModeEnabled) "DEBUG" else "INFO"
    @get:JsonIgnore val androidDevice get() = AndroidDevice.listAll(false).find { it.adbSerial == lastDeviceSerial }

    //<editor-fold desc="Properties">

    val currentProfileProperty = currentProfile.toProperty()
    val sikulixJarPathProperty = sikulixJarPath.toProperty()
    val assetsDirectoryProperty = assetsDirectory.toProperty()
    val clearConsoleOnStartProperty = clearConsoleOnStart.toProperty()
    val debugModeEnabledProperty = debugModeEnabled.toProperty()
    val lastDeviceSerialProperty = lastDeviceSerial.toProperty()

    var currentProfile by currentProfileProperty
    var sikulixJarPath by sikulixJarPathProperty
    var assetsDirectory by assetsDirectoryProperty
    var clearConsoleOnStart by clearConsoleOnStartProperty
    var debugModeEnabled by debugModeEnabledProperty
    var lastDeviceSerial by lastDeviceSerialProperty

    //</editor-fold>

    companion object Loader {
        private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule().ignoreJavaFXPropertyTypes()
        private val loaderLogger = loggerFor<Loader>()

        val path: Path = Wai2K.CONFIG_DIR.resolve("preferences.yml")

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
                }
            } catch (e: JsonMappingException) {
                if (e.message?.startsWith("No content to map due to end-of-input") == false) {
                    loaderLogger.warn("Error occurred while loading the config: ${e.message}")
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

    fun sikulixJarIsValid(): Boolean {
        if (Files.exists(sikulixJarPath) && Files.isRegularFile(sikulixJarPath)) {
            val manifest = JarFile(sikulixJarPath.toFile()).manifest
            return manifest.mainAttributes.getValue("Main-Class") == "org.sikuli.ide.Sikulix"
        }
        return false
    }

    fun save() {
        logger.info("Saving Wai2K configuration file")
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this)
        logger.info("Saving Wai2K configuration was successful")
        printDebugInfo()
    }

    private fun printDebugInfo() {
        logger.debug("Config path: $path")
        logger.debug("Config:\n$this")
    }

    override fun toString(): String = mapper.writeValueAsString(this)
}
