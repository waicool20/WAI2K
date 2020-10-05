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

class GameState {
    var requiresUpdate: Boolean = true
    var requiresRestart: Boolean = false
    var requiresMapInit: Boolean = true

    var dollOverflow: Boolean = false
    var equipOverflow: Boolean = false
    var switchDolls: Boolean = false
    var currentGameLocation: GameLocation = GameLocation(LocationId.UNKNOWN)
    val echelons: List<Echelon> = List(10) { Echelon(it + 1) }
    var delayCoefficient = 1.0

    fun reset() {
        requiresUpdate = true
        requiresRestart = false
        requiresMapInit = true

        dollOverflow = false
        equipOverflow = false
        switchDolls = false

        currentGameLocation = GameLocation(LocationId.UNKNOWN)
        echelons.forEach {
            it.members.forEach { it.needsRepair = false }
            it.logisticsSupportEnabled = true
        }
        delayCoefficient = 1.0
    }

    fun resetAll() {
        reset()
        echelons.forEach {
            it.logisticsSupportAssignment = null
            it.members.forEach { it.repairEta = null }
        }
    }
}