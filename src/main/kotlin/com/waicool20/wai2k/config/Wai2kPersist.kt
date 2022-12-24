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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.waicool20.wai2k.Wai2k
import com.waicool20.waicoolutils.logging.loggerFor
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.notExists

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
class Wai2kPersist(val data: JsonNode = mapper.createObjectNode()) {
    companion object Loader {
        private val loaderLogger = loggerFor<Loader>()
        val mapper = jacksonObjectMapper()

        val path: Path = Wai2k.CONFIG_DIR.resolve("persist.json")
        fun load(): Wai2kPersist {
            loaderLogger.info("Attempting to load WAI2K configuration")
            loaderLogger.debug("Persist data path: $path")
            if (path.notExists()) {
                loaderLogger.info("Persist file not found, creating empty file")
                path.parent.createDirectories()
                path.createFile()
            }

            return try {
                val tree = mapper.readTree(path.toFile())
                loaderLogger.info("WAI2K persist data loaded")
                if (tree.isObject) {
                    Wai2kPersist(tree)
                } else {
                    loaderLogger.warn("Malformed persist data, using empty one")
                    Wai2kPersist()
                }
            } catch (e: JsonMappingException) {
                if (e.message?.startsWith("No content to map due to end-of-input") == false) {
                    throw e
                }
                loaderLogger.info("Using empty persist data")
                Wai2kPersist()
            }
        }
    }

    inner class Writer {
        fun put(key: String, value: Any) {
            check(data is ObjectNode)
            val child = mapper.valueToTree<JsonNode>(value)
            data.set<JsonNode>(key, child)
        }
    }

    inline fun <reified T> get(key: String): T? {
        val node = data.get(key) ?: return null
        return mapper.treeToValue(node)
    }

    fun writer(writerAction: Writer.() -> Unit) {
        val writer = Writer()
        writer.apply(writerAction)
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data)
    }
}
