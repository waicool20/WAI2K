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

import kotlin.math.abs

data class VersionInfo(val version: String = "Unknown") : Comparable<VersionInfo> {
    override fun equals(other: Any?) =
        other != null && other is VersionInfo && compareTo(other) == 0

    override fun hashCode() = super.hashCode()

    override fun compareTo(other: VersionInfo): Int {
        var tokens1 = version.split("\\D".toRegex()).mapNotNull { it.toIntOrNull() }
        var tokens2 = other.version.split("\\D".toRegex()).mapNotNull { it.toIntOrNull() }
        val diff = abs(tokens1.size - tokens2.size)
        if (tokens1.size > tokens2.size) {
            tokens2 += List(diff) { 0 }
        } else {
            tokens1 += List(diff) { 0 }
        }
        tokens1.zip(tokens2).forEach { (first, second) ->
            when {
                first > second -> return 1
                first < second -> return -1
            }
        }
        return 0
    }
}
