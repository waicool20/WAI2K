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

package com.waicool20.wai2k.game

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.util.Ocr
import com.waicool20.waicoolutils.distanceTo

data class TDoll(
    /**
     * Display name of the tdoll as shown in game
     */
    val name: String,
    val stars: Int,
    val type: Type,
    private val moddable: Boolean = false
) {
    /**
     * Unique tdoll id, usually just the tdoll name with + appended to it if it's mod variant
     */
    var id: String = name
        private set

    companion object {
        private val list: MutableList<TDoll> = ArrayList()

        /**
         * Returns a list with all tdolls
         */
        fun listAll(config: Wai2KConfig): List<TDoll> = synchronized(list) {
            if (list.isEmpty()) {
                list += jacksonObjectMapper().readValue<List<TDoll>>(config.assetsDirectory.resolve("tdolls.json").toFile())
                list += list.filter { it.moddable }.map { it.copy(stars = (it.stars + 1).coerceAtLeast(4)).apply { id = "$name+" } }
            }
            return list
        }

        /**
         * Look for the closest matching tdoll with the given name
         */
        fun lookup(config: Wai2KConfig, name: String?): TDoll? {
            if (name == null) return null
            return listAll(config).find { it.id == name } ?: listAll(config).find {
                var score = (name.length - it.name.distanceTo(name, Ocr.OCR_DISTANCE_MAP)) / name.length
                // Be a bit more lax on longer names
                if (name.length > 6) score *= 1.2
                score > config.scriptConfig.ocrThreshold
            }
        }
    }

    enum class Type {
        HG, SMG, RF, AR, MG, SG
    }
}