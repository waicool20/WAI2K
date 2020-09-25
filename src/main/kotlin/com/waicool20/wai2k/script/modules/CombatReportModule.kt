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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.config.Wai2KProfile.CombatReport
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.wai2k.util.formatted
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

class CombatReportModule(
    scriptRunner: ScriptRunner,
    region: AndroidRegion,
    config: Wai2KConfig,
    profile: Wai2KProfile,
    navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {
    private val logger = loggerFor<CombatReportModule>()

    private var lastCheck = Instant.now()

    override suspend fun execute() {
        if (!profile.combat.enabled) return
        if (!profile.combatReport.enabled) return
        if (ChronoUnit.SECONDS.between(lastCheck, Instant.now()) < 3600) return
        overworkKalina()
    }

    private suspend fun overworkKalina() {
        navigator.navigateTo(LocationId.DATA_ROOM)
        // Wait a bit since loading the data room takes some time
        val desk = region.waitHas(FileTemplate("combat-report/desk.png"), 10000) ?: run {
            logger.warn("Could not find Kalina's desk after 10s")
            return
        }
        if (region.has(FileTemplate("combat-report/working.png"))) {
            logger.info("Kalina is already on overtime!")
            return
        }
        logger.info("Making Kalina work overtime")
        // Move a bit to the left so it doesn't click the hard disk array
        desk.copy(x = desk.x - 20).click()
        delay(1000)
        // Click work button
        region.subRegion(1510, 568, 277, 86).click(); delay(500)
        // Select type
        val reportRegion = when (profile.combatReport.type) {
            CombatReport.Type.NORMAL -> {
                logger.info("Selecting normal combat reports")
                region.subRegion(583, 399, 465, 148).click()
                region.subRegion(842, 474, 115, 48)
            }
            CombatReport.Type.SPECIAL -> {
                logger.info("Selecting special combat reports")
                region.subRegion(1160, 399, 465, 148).click()
                region.subRegion(1420, 474, 115, 48)
            }
            else -> error("No such combat report type!")
        }
        delay(500)
        val reports = Ocr.forConfig(config).doOCRAndTrim(reportRegion)
            .takeWhile { it.isDigit() || it != '/' }
            .toIntOrNull()?.coerceAtMost(80)
        if (reports != null) {
            logger.info("Writing $reports reports")
            scriptStats.combatReportsWritten += reports
        } else {
            logger.warn("Could not determine amount of reports to write")
        }
        logger.info("Confirming selection")
        region.subRegion(1144, 749, 268, 103).click()
        lastCheck = Instant.now()
        logger.info("Next check is in one hour (${lastCheck.formatted()})")
    }
}