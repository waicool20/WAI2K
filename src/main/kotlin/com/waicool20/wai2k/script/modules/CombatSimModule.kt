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
import com.waicool20.wai2k.config.Wai2KProfile.CombatSimulation.Level
import com.waicool20.wai2k.game.Echelon
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptException
import com.waicool20.wai2k.script.ScriptTimeOutException
import com.waicool20.wai2k.script.modules.combat.AbsoluteMapRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.wai2k.util.formatted
import com.waicool20.wai2k.util.useCharFilter
import com.waicool20.waicoolutils.DurationUtils
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.prettyString
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import java.time.*
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong
import kotlin.random.Random

class CombatSimModule(navigator: Navigator) : ScriptModule(navigator) {

    private val logger = loggerFor<CombatReportModule>()
    private val dataSimDays = arrayOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
    private var nextCheck = Instant.now()
    private var energyRemaining = 0
    private var rechargeTime = Duration.ZERO

    private val mapRunner = object : AbsoluteMapRunner(this@CombatSimModule) {
            override suspend fun begin() {
            if (profile.combatSimulation.neuralFragment == Level.OFF) return

            if (OffsetDateTime.now(ZoneOffset.ofHours(-8)).dayOfWeek !in dataSimDays) {
                nextCheck = Instant.now().plus(1, ChronoUnit.DAYS)
            }

            val level = profile.combatSimulation.neuralFragment
            var times = energyRemaining / level.cost
            if (times == 0) {
                updateNextCheck(level)
                return
            }

            region.findBest(FileTemplate("combat-simulation/neural.png"))?.region?.click()
            delay((1000 * gameState.delayCoefficient).roundToLong())

            logger.info("Running neural sim type $level $times times")
            logger.info("Entering $level sim")
            region.subRegion(735, 377 + (177 * (level.cost - 1)), 1230, 130).click() // Difficulty
            delay(1000)

            var energySpent = 0
            // Heliport that turns into a node
            val heliport = region.subRegion(1055, 1000, 24, 24)
            // blank mode at the end with SF on it
            val endNode = region.subRegion(1105, 565, 24, 24)
            // echelon to use for the run
            val echelon = Echelon(number = profile.combatSimulation.neuralEchelon)


            // Plan the route on the first run
            region.subRegion(1730, 900, 430, 180)
                .waitHas(FileTemplate("combat/battle/start.png"), 10000)
            delay(1000)


            logger.info("Zoom out")
            repeat(2) {
                region.pinch(
                    Random.nextInt(900, 1000),
                    Random.nextInt(300, 400),
                    0.0,
                    500
                )
                delay(500)
            }
            logger.info("Pan up")
            val r = region.subRegionAs<AndroidRegion>(700, 140, 400, 100)
            r.swipeTo(r.copy(y = r.y + 400))
            delay(1000) // Wait to settle

            heliport.click()
            region.waitHas(FileTemplate("ok.png"), 3000)
            val deploy = clickEchelon(echelon)
            if (!deploy) {
                throw ScriptException("Could not deploy echelon $echelon")
            }
            delay(1000)

            // A pretty common script restart is if you get a long loadwheel from poor connection
            // and the try to .click() start the (unavailable) button it will get stuck until timeout
            mapRunnerRegions.deploy.click(); delay(1000)
            mapRunnerRegions.startOperation.click(); delay(1000)
            try {
                region.subRegion(1100, 680, 275, 130)
                    .waitHas(FileTemplate("ok.png"), 5000)?.click()
            } catch (e: TimeoutCancellationException) {
                throw ScriptTimeOutException("Could not start neural sim")
            }
            waitForGNKSplash(7000) // Map background makes it hard to find
            mapRunnerRegions.planningMode.click(); delay(500)
            heliport.click(); delay(500)
            endNode.click(); delay(500)
            mapRunnerRegions.executePlan.click(); delay(7000)
            waitForTurnEnd(1, timeout = 60_000)

            while (true) {
                mapRunnerRegions.battleEndClick.click()
                delay(300)
                if (GameLocation.mappings(config)[LocationId.COMBAT_SIMULATION]!!.isInRegion(region)) {
                    energySpent += level.cost
                    break
                }
                if (region.subRegion(1100, 680, 275, 130).has(FileTemplate("ok.png"))) {
                    energySpent += level.cost
                    if (--times == 0) {
                        region.subRegion(788, 695, 250, 96).click() // Cancel
                        break
                    } else {
                        region.subRegion(1115, 695, 250, 96).click() // ok
                        logger.info("Done one cycle, remaining: $times")
                        delay(7000)
                        waitForTurnEnd(1, timeout = 60_000)
                    }
                }
            }
            logger.info("Completed all neural sim")
            scriptStats.simEnergySpent += energySpent
            energyRemaining -= energySpent
            updateNextCheck(level)
        }
    }

    override suspend fun execute() {
        if (!profile.combatSimulation.enabled) return
        if (Instant.now() < nextCheck) return
        if (!combatSimAvailable()) return
        checkSimEnergy()
        runDataSimulation()
        runNeuralFragment()
        logger.info("Sim energy remaining : $energyRemaining")
        logger.info("Next sim check at: ${nextCheck.formatted()}")
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

        while (true) {
            val energyString = Ocr.forConfig(config)
                .useCharFilter("0123456/")
                .doOCRAndTrim(region.subRegion(1455, 165, 75, 75))
                .replace(" ", "")
            logger.info("Sim energy OCR: $energyString")

            // Continue if not in X/6 format
            if (!energyString.matches(Regex("\\d/6"))) {
                delay(500)
                continue
            }

            energyRemaining = energyString.take(1).toIntOrNull() ?: continue

            logger.info("Current sim energy is $energyRemaining/6")

            if (energyRemaining == 6) {
                rechargeTime = Duration.ZERO
                return
            }
            break
        }

        while (true) {
            val timerString = Ocr.forConfig(config)
                .useCharFilter("0123456789:")
                .doOCRAndTrim(region.subRegion(1552, 182, 100, 45))
                .replace(" ", "")
            logger.info("Sim timer OCR: $timerString")

            if (!timerString.matches(Regex("\\d:\\d\\d:\\d\\d"))) {
                delay(500)
                continue
            }

            var seconds = timerString.substring(5, 7)
            var minutes = timerString.substring(2, 4)
            var hours = timerString.substring(0, 1)

            if (seconds[0] == '8') seconds = seconds.replaceFirst('8', '0')
            if (minutes[0] == '8') minutes = minutes.replaceFirst('8', '0')
            if (hours[0] == '8') hours = hours.replaceFirst('8', '0')

            rechargeTime = DurationUtils.of(
                seconds.toLong(),
                minutes.toLong(),
                hours.toLong()
            )
            logger.info("Time until next sim energy: ${rechargeTime.prettyString()}")
            return
        }
    }

    private suspend fun runDataSimulation() {
        if (profile.combatSimulation.dataSim == Level.OFF) return
        if (OffsetDateTime.now(ZoneOffset.ofHours(-8)).dayOfWeek !in dataSimDays) return

        val level = profile.combatSimulation.dataSim
        var times = energyRemaining / level.cost
        if (times == 0) {
            updateNextCheck(level)
            return
        }

        region.findBest(FileTemplate("combat-simulation/data-mode.png"))?.region?.click()
        // Generous Delays here since combat sims don't occur often
        delay((1000 * gameState.delayCoefficient).roundToLong())

        logger.info("Running data sim type $level $times times")
        region.subRegion(735, 377 + (177 * (level.cost - 1)), 1230, 130).click() // Difficulty
        delay(1000)
        logger.info("Entering $level sim")
        region.subRegion(1320, 810, 300, 105).click() // Enter Combat
        region.waitHas(FileTemplate("ok.png"), 5000)?.click()
        delay(3000)

        logger.info("Clicking through sim results")

        var energySpent = 0
        while (true) {
            region.subRegion(992, 24, 1100, 121).click() // endBattleClick
            delay(300)
            if (GameLocation.mappings(config)[LocationId.COMBAT_SIMULATION]!!.isInRegion(region)) {
                energySpent += level.cost
                break
            }
            if (region.subRegion(1100, 680, 275, 130).has(FileTemplate("ok.png"))) {
                energySpent += level.cost
                if (--times == 0) {
                    region.subRegion(788, 695, 250, 96).click() // Cancel
                    break
                } else {
                    region.subRegion(1115, 695, 250, 96).click() // ok
                    logger.info("Done one cycle, remaining: $times")
                }
            }
        }
        logger.info("Completed all data sim")
        scriptStats.simEnergySpent += energySpent
        energyRemaining -= energySpent
        updateNextCheck(level)
    }

    private suspend fun runNeuralFragment() {
        mapRunner.execute()
    }

    /**
     * Schedules the next check up time based on the required level passed in
     */
    private fun updateNextCheck(level: Level) {
        nextCheck = Instant.now().plusSeconds(((level.cost - energyRemaining - 1) * 7200) + rechargeTime.seconds)
    }
}