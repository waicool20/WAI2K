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

package com.waicool20.wai2k.script.modules

import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.Navigator
import com.waicool20.waicoolutils.logging.loggerFor
import java.time.*

class StopModule(navigator: Navigator) : ScriptModule(navigator) {
    private val logger = loggerFor<StopModule>()

    override suspend fun execute() {
        if (!profile.stop.enabled) return
        checkTime()
        checkCount()
    }

    //<editor-fold desc="Time">

    private var nextStopTime = getNextTime(profile.stop.time.specificTime)

    private suspend fun checkTime() {
        with(profile.stop.time) {
            if (!enabled) return
            val stop = when (mode) {
                Wai2KProfile.Stop.Time.Mode.ELAPSED_TIME -> {
                    Duration.between(scriptRunner.lastStartTime!!, Instant.now()) > elapsedTime
                }
                Wai2KProfile.Stop.Time.Mode.SPECIFIC_TIME -> {
                    LocalDateTime.now() > nextStopTime
                }
                else -> false
            }
            if (stop) stopScript("$mode")
        }
    }

    private fun getNextTime(time: LocalTime): LocalDateTime {
        val now = LocalDateTime.now()
        val i = time.atDate(LocalDate.now())
        return if (i.isBefore(now)) i.plusDays(1) else i
    }

    //</editor-fold>

    private suspend fun checkCount() {
        with(profile.stop.count) {
            if (!enabled) return
            if (scriptStats.sortiesDone >= sorties) stopScript("Sorties >= $sorties")
        }
    }
}