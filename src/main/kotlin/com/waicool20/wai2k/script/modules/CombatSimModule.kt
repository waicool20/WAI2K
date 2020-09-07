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
import com.waicool20.cvauto.core.asCachedRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.config.Wai2KProfile.CombatSimulation.Level
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.wai2k.util.useCharFilter
import com.waicool20.waicoolutils.DurationUtils
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

class CombatSimModule(
    scriptRunner: ScriptRunner,
    region: AndroidRegion,
    config: Wai2KConfig,
    profile: Wai2KProfile,
    navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {

    private val logger = loggerFor<CombatReportModule>()
    private var nextCheck = Instant.now()
    private var energy = 0
    private var rechargeTime = Duration.ofSeconds(0)
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    override suspend fun execute() {
        if (!profile.combatSimulation.enabled) return
        if (Instant.now() < nextCheck) return
        val daysOpen = arrayOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
        if (OffsetDateTime.now(ZoneOffset.ofHours(-8)).dayOfWeek !in daysOpen) return
        checkSimEnergy()
        while (energy > 0) {
            runDataSimulation()
        }
        logger.info("Next Sim check is in: ${nextCheck.toString().format(formatter)}")
    }

    private suspend fun checkSimEnergy() {
        // Check the current sim energy and the duration until the next energy recharges
        // Perhaps put this in gameState if it didn't take so long to check
        navigator.navigateTo(LocationId.COMBAT_SIMULATION)

        // X/6 xx:xx:xx part
        val simEnergyRegion = region.subRegion(1462, 184, 188, 44).asCachedRegion()
        energy = Ocr.forConfig(config)
            .useCharFilter("0123456")
            .doOCRAndTrim(simEnergyRegion)
            .replace(" ", "")
            .takeIf { it.isNotEmpty() }
            ?.take(1)
            ?.toInt() ?: 0 // If there is lag or something blocking

        logger.info("Current sim energy is $energy/6")

        var remainder = Ocr.forConfig(config)
            .useCharFilter("0123456/")
            .doOCRAndTrim(simEnergyRegion)
            .replace(" ", "")
            .takeLast(6)

        // If energy is at 6/6 it will be --:--:--
        if (remainder.length < 6) {
            remainder = "000000"
        }

        rechargeTime = DurationUtils.of(
            remainder.substring(4, 6).toLong(),
            remainder.substring(2, 4).toLong(),
            remainder.substring(0, 2).toLong()
        )
        logger.info("Time until next sim energy ${rechargeTime.seconds.toString().format(formatter)}")
        nextCheck = Instant.now().plusSeconds(rechargeTime.seconds)
    }

    private suspend fun runDataSimulation() {
        // Runs a training data sim, prioritising the highest difficulty enabled in config

        region.subRegion(400, 320, 180, 120).click()
        // Generous Delays here since combat sims don't occur often
        delay((1000 * gameState.delayCoefficient).roundToLong())

        val cost = profile.combatSimulation.dataSim.cost
        if (cost == 0) {
            logger.info("Not enough energy to run selected simulations")
        } else {
            logger.info("Selecting data sim type")
            region.subRegion(735, 377 + (177 * (cost - 1)), 1230, 130).click() // Difficulty
            delay(1000)
            logger.info("Entering sim")
            region.subRegion(1320, 810, 300, 105).click() // Enter Combat
            region.waitHas(FileTemplate("ok.png"), 5000)?.click()
            delay(3000)

            logger.info("Clicking through sim results")
            // If there is enough sim energy (including extra energy) to run again
            // the previous message box will appear again
            // Perhaps make this more like mapRunner
            withTimeout(8000) {
                while (region.subRegion(1959, 179, 60, 60)
                        .doesntHave(FileTemplate("locations/landmarks/combat_simulation.png"))
                ) {
                    region.subRegion(992, 24, 1100, 121).click() // endBattleClick
                    delay(300)
                    region.subRegion(761, 674, 283, 144)
                        .findBest(FileTemplate("cancel-logi.png"))?.region?.click() // Same button
                }
            }
        }
        scriptStats.simEnergySpent += cost
        energy -= cost
        logger.info("Sim energy remaining : $energy")
        nextCheck = Instant.now().plusSeconds(((cost - energy) * 7200) - rechargeTime.seconds)
    }
}