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
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.wai2k.Wai2K
import com.waicool20.wai2k.game.DollType
import com.waicool20.waicoolutils.javafx.json.fxJacksonObjectMapper
import com.waicool20.waicoolutils.javafx.toProperty
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Wai2KProfile(
        val logistics: Logistics = Logistics(),
        val combat: Combat = Combat(),
        val factory: Factory = Factory()
) {

    constructor(name: String, logistics: Logistics) : this(logistics) {
        this.name = name
    }

    data class DollCriteria(var name: String, var level: Int, var stars: Int, var type: DollType)

    class Logistics(
            enabled: Boolean = false,
            receivalMode: ReceivalMode = ReceivalMode.RANDOM,
            assignments: MutableMap<Int, ListProperty<Int>> = (1..10).associateWith {
                SimpleListProperty<Int>(ArrayList<Int>().observable())
            }.toMutableMap()
    ) {
        enum class ReceivalMode {
            ALWAYS_CONTINUE, RANDOM, ALWAYS_CANCEL
        }

        val enabledProperty = enabled.toProperty()
        val assignmentsProperty = assignments.toProperty()
        val receiveModeProperty = receivalMode.toProperty()
        var assignments by assignmentsProperty
        var receiveMode by receiveModeProperty
        val enabled by enabledProperty
    }

    class Combat(
            enabled: Boolean = false,
            map: String = "4-3E",
            draggers: MutableMap<Int, DollCriteria> = mutableMapOf(
                    1 to DollCriteria("WA2000", 100, 5, DollType.RF),
                    2 to DollCriteria("FAL", 100, 5, DollType.AR)
            )
    ) {
        val enabledProperty = enabled.toProperty()
        val mapProperty = map.toProperty()
        val draggersProperty = draggers.toProperty()

        val enabled by enabledProperty
        val map by mapProperty
        val draggers by draggersProperty
    }

    class Factory(
            enhancement: Enhancement = Enhancement(),
            disassembly: Disassembly = Disassembly()
    ) {
        class Enhancement(
                enabled: Boolean = true
        ) {
            val enabledProperty = enabled.toProperty()

            val enabled by enabledProperty
        }

        class Disassembly(
                enabled: Boolean = false
        ) {
            val enabledProperty = enabled.toProperty()

            val enabled by enabledProperty
        }

        val enhancementProperty = enhancement.toProperty()
        val disassemblyProperty = disassembly.toProperty()

        val enhancement by enhancementProperty
        val disassembly by disassemblyProperty
    }

    companion object Loader {
        private val loaderLogger = loggerFor<Loader>()
        private val mapper = fxJacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val PROFILE_DIR: Path = Wai2K.CONFIG_DIR.resolve("profiles")
        const val DEFAULT_NAME = "Default"

        fun profileExists(name: String) = Files.exists(PROFILE_DIR.resolve("$name${Wai2K.CONFIG_SUFFIX}"))

        fun load(name: String): Wai2KProfile {
            return load(PROFILE_DIR.resolve("${name.takeIf { it.isNotBlank() }
                    ?: DEFAULT_NAME}${Wai2K.CONFIG_SUFFIX}")).also { it.name = name }
        }

        fun load(path: Path): Wai2KProfile {
            loaderLogger.info("Attempting to load profile")
            loaderLogger.debug("Profile path: $path")
            if (Files.notExists(path)) {
                loaderLogger.info("Profile not found, creating empty file")
                Files.createDirectories(PROFILE_DIR)
                Files.createFile(path)
            }

            return try {
                mapper.readValue<Wai2KProfile>(path.toFile()).apply {
                    loaderLogger.info("Profile loaded")
                    name = "${path.fileName}".removeSuffix(Wai2K.CONFIG_SUFFIX)
                    printDebugInfo()
                }
            } catch (e: JsonMappingException) {
                if (e.message?.startsWith("No content to map due to end-of-input") == false) {
                    loaderLogger.warn("Error occurred while loading the profile: ${e.message}")
                    throw e
                }
                loaderLogger.info("Using default profile")
                Wai2KProfile().apply { save() }
            }
        }
    }

    private val logger = loggerFor<Wai2KProfile>()

    val nameProperty = DEFAULT_NAME.toProperty()
    @get:JsonIgnore var name by nameProperty

    @get:JsonIgnore
    val path: Path
        get() = PROFILE_DIR.resolve("$name${Wai2K.CONFIG_SUFFIX}")

    fun save(path: Path = this.path) {
        logger.info("Saving Wai2K profile")
        if (Files.notExists(path)) {
            logger.debug("Profile not found, creating file $path")
            Files.createDirectories(path.parent)
            Files.createFile(path)
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this)
        logger.info("Profile saved!")
        printDebugInfo()
    }

    fun delete() {
        if (Files.exists(path)) {
            Files.delete(path)
            logger.info("Profile deleted")
            printDebugInfo()
        }
    }

    private fun printDebugInfo() {
        logger.debug("Profile path: $path")
        logger.debug("Profile:\n$this")
    }

    override fun toString(): String = mapper.writeValueAsString(this)
}
