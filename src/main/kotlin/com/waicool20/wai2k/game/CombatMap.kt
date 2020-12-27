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

import com.waicool20.wai2k.script.InvalidMapNameException

sealed class CombatMap(val name: String) {
    enum class Type {
        NORMAL, EMERGENCY, NIGHT
    }

    class StoryMap(name: String) : CombatMap(name) {
        companion object {
            private val regex = Regex("(\\d{1,2})-(\\d)([eEnN]?)-?(.*)?")
        }

        private val matches: List<String> =
            regex.matchEntire(name)?.groupValues ?: throw InvalidMapNameException(name)
        val chapter: Int = matches[1].toInt()
        val number: Int = matches[2].toInt()
        val type: Type = when (matches.getOrNull(3)) {
            "e", "E" -> Type.EMERGENCY
            "n", "N" -> Type.NIGHT
            else -> Type.NORMAL
        }

        override fun toString() = "StoryMap(name=$name)"
    }

    class EventMap(name: String) : CombatMap(name) {
        override fun toString() = "EventMap(name=$name)"
    }

    class CampaignMap(name: String) : CombatMap(name) {
        override fun toString() = "CampaignMap(name=$name)"
    }
}