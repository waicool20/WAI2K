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
import com.waicool20.wai2k.config.Wai2KProfile.CombatSimulation.Level
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.wai2k.util.formatted
import com.waicool20.waicoolutils.DurationUtils
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.prettyString
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.time.*
import kotlin.math.roundToLong

class CombatSimModule(
    scriptRunner: ScriptRunner,
    region: AndroidRegion,
    config: Wai2KConfig,
    profile: Wai2KProfile,
    navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {

    private val logger = loggerFor<CombatReportModule>()
    private val dataSimDays = arrayOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
    private var nextCheck = Instant.now()
    private var energy = 0
    private var rechargeTime = Duration.ZERO

    override suspend fun execute() {
        if (!profile.combatSimulation.enabled) return
        if (Instant.now() < nextCheck) return
        if (!combatSimAvailable()) return
        checkSimEnergy()
        while (energy > 0) {
            if (runDataSimulation()) continue
            runNeuralFragment()
        }
        logger.info("Next sim check is in: ${nextCheck.formatted()}")
    }

    /**
     * Returns true if any enabled combat simulation is available that day
     */
    private fun combatSimAvailable(): Boolean {
        val daysOpen = mutableSetOf<DayOfWeek>()
        if (profile.combatSimulation.dataSim != Level.OFF) daysOpen += dataSimDays
        if (profile.combatSimulation.neuralFragment != Level.OFF) daysOpen += DayOfWeek.values()
        return OffsetDateTime.now(ZoneOffset.ofHours(-8)).dayOfWeek in daysOpen
    }

    private suspend fun checkSimEnergy() {
        // Check the current sim energy and the duration until the next energy recharges
        // Perhaps put this in gameState if it didn't take so long to check
        navigator.navigateTo(LocationId.COMBAT_SIMULATION)

        // X/6 HH:mm:ss part
        val simEnergyRegion = region.subRegion(1462, 184, 188, 44)

        while (true) {
            val simString = Ocr.forConfig(config, digitsOnly = true)
                .doOCRAndTrim(simEnergyRegion)
                .replace(" ", "")
            logger.info("Sim energy OCR: $simString")

            // Return if not in X6HHmmss format or empty
            if (simString.isEmpty() || simString.length != 8) {
                delay(500)
                continue
            }

            energy = simString.replace("8", "0")
                .take(1).toIntOrNull()?.takeIf { it in 0..6 } ?: continue // If there is lag or something blocking

            logger.info("Current sim energy is $energy/6")

            if (energy == 6) {
                rechargeTime = Duration.ZERO
                return
            }

            var seconds = simString.substring(6, 8)
            var minutes = simString.substring(4, 6)
            val hours = simString.substring(3, 4)

            if (seconds[0] == '8') seconds = seconds.replaceFirst('8', '0')
            if (minutes[0] == '8') minutes = minutes.replaceFirst('8', '0')

            rechargeTime = DurationUtils.of(
                seconds.toLong(),
                minutes.toLong(),
                hours.toLong()
            )
            logger.info("Time until next sim energy: ${rechargeTime.prettyString()}")
            return
        }
    }

    private suspend fun runDataSimulation(): Boolean {
        if (profile.combatSimulation.dataSim == Level.OFF) return false
        if (OffsetDateTime.now(ZoneOffset.ofHours(-8)).dayOfWeek !in dataSimDays) return false

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
        return true
    }

    /**
     * Placeholder function
     */
    private suspend fun runNeuralFragment() {
        //TODO
    }
}