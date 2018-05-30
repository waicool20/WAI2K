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

package com.waicool20.wai2k.android.input

import com.waicool20.wai2k.android.enums.InputEvent

/**
 * Represents an input device on the android device
 *
 * @param devFile The device file, usually /dev/input/eventX
 * @param name Name of the input device
 * @param specs Specification of the acceptable touch values, see "ABS_MT_XXXX" in [InputEvent]
 */
data class AndroidInput(
        val devFile: String,
        val name: String,
        val specs: Map<InputEvent, TouchSpec>
) {
    companion object {
        fun parse(deviceInfo: String): AndroidInput {
            val lines = deviceInfo.lines()
            val devFile = lines[0].takeLastWhile { it != ' ' }
            val name = lines[1].dropWhile { it != '"' }.removeSurrounding("\"")
            val specs = lines.subList(2, lines.lastIndex)
                    .mapNotNull { TouchSpec.REGEX.matchEntire(it)?.groupValues }
                    .mapNotNull {
                        InputEvent.findByCode(it[1].toLong(16))?.let { code ->
                            code to TouchSpec(it[2].toInt(), it[3].toInt(), it[4].toInt(), it[5].toInt(), it[6].toInt())
                        }
                    }.toMap()
            return AndroidInput(devFile, name, specs)
        }
    }
}

/**
 * Specification of the acceptable touch values, see "ABS_MT_XXXX" in [InputEvent]
 *
 * @param minValue Minimum value
 * @param maxValue Maximum value
 * @param fuzz
 * @param flat
 * @param resolution
 */
data class TouchSpec(
        val minValue: Int,
        val maxValue: Int,
        val fuzz: Int,
        val flat: Int,
        val resolution: Int
) {
    companion object {
        val REGEX = Regex(".*?(\\w{4})\\s+:\\s+value \\d+, min (\\d+), max (\\d+), fuzz (\\d+), flat (\\d+), resolution (\\d+).*?")
    }
}
