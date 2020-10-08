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

package com.waicool20.wai2k.script

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class ScriptStats(
    var logisticsSupportReceived: Int = 0,
    var logisticsSupportSent: Int = 0,
    var sortiesDone: Int = 0,
    var enhancementsDone: Int = 0,
    var dollsUsedForEnhancement: Int = 0,
    var disassemblesDone: Int = 0,
    var dollsUsedForDisassembly: Int = 0,
    var equipDisassemblesDone: Int = 0,
    var equipsUsedForDisassembly: Int = 0,
    var repairs: Int = 0,
    var gameRestarts: Int = 0,
    var combatReportsWritten: Int = 0,
    var simEnergySpent: Int = 0
) {
    fun reset() {
        logisticsSupportReceived = 0
        logisticsSupportSent = 0
        sortiesDone = 0
        enhancementsDone = 0
        dollsUsedForEnhancement = 0
        disassemblesDone = 0
        dollsUsedForDisassembly = 0
        equipDisassemblesDone = 0
        equipsUsedForDisassembly = 0
        repairs = 0
        gameRestarts = 0
        combatReportsWritten = 0
        simEnergySpent = 0
    }

    override fun toString(): String = jacksonObjectMapper().writerWithDefaultPrettyPrinter()
        .writeValueAsString(this)
}