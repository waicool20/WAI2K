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

import com.waicool20.cvauto.core.AnyRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.config.Wai2kProfile.CombatSimulation.Coalition.Type
import com.waicool20.wai2k.config.Wai2kProfile.CombatSimulation.Level
import com.waicool20.wai2k.events.CoalitionEnergySpentEvent
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.events.SimEnergySpentEvent
import com.waicool20.wai2k.game.Echelon
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptException
import com.waicool20.wai2k.script.ScriptTimeOutException
import com.waicool20.wai2k.script.modules.combat.EmptyMapRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.isSimilar
import com.waicool20.wai2k.util.readText
import com.waicool20.wai2k.util.useCharFilter
import com.waicool20.waicoolutils.DurationUtils
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.prettyString
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.awt.Color
import java.time.*
import java.time.temporal.ChronoUnit
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong
import kotlin.random.Random

class CombatSimModule(navigator: Navigator) : ScriptModule(navigator) {

    private val logger = loggerFor<CombatReportModule>()
    private val dataSimDays = arrayOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
    private var simTimer = Duration.ZERO
    private var coalitionTimer = Duration.ZERO

    init {
        logger.warn(
            """
            
            
            ####################################
            NOTICE: 
            Combat Sim module is disabled until MICA fixes their broken ass fonts
            ####################################
            
            
            """.trimIndent()
        )
    }

    private val mapRunner = object : EmptyMapRunner(this@CombatSimModule) {
        override suspend fun begin() {
            if (profile.combatSimulation.neuralFragment == Level.OFF) return

            if (OffsetDateTime.now(ZoneOffset.ofHours(-8)).dayOfWeek !in dataSimDays) {
                gameState.simNextCheck = Instant.now().plus(1, ChronoUnit.DAYS)
            }

            val level = profile.combatSimulation.neuralFragment
            var times = gameState.simEnergy / level.cost
            if (times == 0) {
                updateSimNextCheck(level)
                return
            }

            region.findBest(FileTemplate("combat-simulation/neural.png", 0.8))?.region?.click()
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
            val r = region.subRegion(700, 140, 400, 100)
            r.swipeTo(r.copy(y = r.y + 400))
            delay(1000) // Wait to settle

            heliport.click()
            region.waitHas(FileTemplate("ok.png"), 3000)
            val deploy = echelon.clickEchelon(this, 140)
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
            enterPlanningMode(); delay(500)
            heliport.click(); delay(500)
            endNode.click(); delay(500)
            mapRunnerRegions.executePlan.click(); delay(7000)
            waitForTurnEnd(1, timeout = 60_000)

            while (true) {
                mapRunnerRegions.battleEndClick.click()
                delay(300)
                if (locations.getValue(LocationId.COMBAT_SIMULATION).isInRegion(region)) {
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
            EventBus.publish(
                SimEnergySpentEvent(
                    "NEURAL_FRAGMENT",
                    level,
                    energySpent,
                    sessionId,
                    elapsedTime
                )
            )
            gameState.simEnergy -= energySpent
            updateSimNextCheck(level)
        }
    }

    override suspend fun execute() {
        return // TODO: Re-enable when fonts are fixed
        if (!profile.combatSimulation.enabled) return
        executeNormal()
        executeCoalition()
    }

    private suspend fun executeNormal() {
        if (Instant.now() < gameState.simNextCheck) return
        if (!combatSimAvailable()) return

        navigator.navigateTo(LocationId.COMBAT_SIMULATION)
        delay(1000) // Delay for settle

        // Select normal combat sim
        region.subRegion(396, 155, 199, 105).click()

        logger.info("Checking sim energy...")
        val (timer, energy) = checkSimEnergy(
            region.subRegion(1455, 165, 75, 75),
            region.subRegion(1552, 182, 100, 45)
        ) ?: return

        simTimer = timer
        gameState.simEnergy = energy
        runDataSimulation()
        runNeuralFragment()
        logger.info("Sim energy remaining : ${gameState.simEnergy}")
    }

    private suspend fun executeCoalition() {
        if (!profile.combatSimulation.coalition.enabled) return
        if (Instant.now() < gameState.coalitionNextCheck) return

        navigator.navigateTo(LocationId.COMBAT_SIMULATION)

        logger.info("Selecting coalition tab")
        while (coroutineContext.isActive) {
            delay(5000)
            region.findBest(FileTemplate("combat-simulation/coalition-drill.png"))?.region?.click()
            if (Color(region.capture().getRGB(1780, 235)).isSimilar(Color(156, 36, 33))) {
                break
            }
        }

        logger.info("Checking coalition energy...")
        val (cTimer, cEnergy) = checkSimEnergy(
            region.subRegion(1468, 165, 75, 75),
            region.subRegion(1542, 182, 115, 45)
        ) ?: return

        coalitionTimer = cTimer
        gameState.coalitionEnergy = cEnergy
        runCoalition()
        logger.info("Coalition energy remaining : ${gameState.coalitionEnergy}")
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

    private suspend fun checkSimEnergy(
        energyRegion: AnyRegion,
        timerRegion: AnyRegion,
        retries: Int = 5
    ): Pair<Duration, Int>? {
        // Check the current sim energy and the duration until the next energy recharges
        var energy = -1
        var r = retries

        while (coroutineContext.isActive) {
            val energyString = ocr
                .useCharFilter("0123456/")
                .readText(energyRegion, scale = 0.8, threshold = 0.7, pad = 50)
                .replace(" ", "")
            logger.info("Sim energy OCR: $energyString")

            // Continue if not in X/6 format
            if (!energyString.matches(Regex("\\d/6"))) {
                if (--r >= 0) {
                    delay(500)
                    continue
                } else return null
            }

            energy = energyString.take(1).toInt()
            break
        }
        logger.info("Current sim energy is ${energy}/6")

        if (energy == 6) {
            return Duration.ZERO to energy
        }

        r = retries
        while (coroutineContext.isActive) {
            val timerString = ocr.useCharFilter(Ocr.DIGITS + ":")
                .readText(timerRegion, scale = 1.2, threshold = 0.7, pad = 50)
            logger.info("Sim timer OCR: $timerString")

            val m = Regex("(\\d?\\d)\\D(\\d\\d)\\D(\\d\\d?)").find(timerString)
                ?: if (--r >= 0) {
                    delay(500)
                    continue
                } else return null

            var (hours, minutes, seconds) = m.destructured

            if (seconds.length == 1) seconds += "0"

            val timer = DurationUtils.of(
                seconds.toLong(),
                minutes.toLong(),
                hours.toLong()
            )
            logger.info("Time until next sim energy: ${timer.prettyString()}")
            return timer to energy
        }
        return null
    }

    private suspend fun runDataSimulation() {
        if (profile.combatSimulation.dataSim == Level.OFF) return
        if (OffsetDateTime.now(ZoneOffset.ofHours(-8)).dayOfWeek !in dataSimDays) return

        val level = profile.combatSimulation.dataSim
        val times = gameState.simEnergy / level.cost
        if (times == 0) {
            updateSimNextCheck(level)
            return
        }

        region.findBest(FileTemplate("combat-simulation/data-mode.png", 0.8))?.region?.click()
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

        runSimCycles(times)
        logger.info("Completed all data sim")
        EventBus.publish(
            SimEnergySpentEvent(
                "DATA_SIMULATION",
                level,
                times * level.cost,
                sessionId,
                elapsedTime
            )
        )
        gameState.simEnergy -= times * level.cost
        updateSimNextCheck(level)
    }

    private suspend fun runNeuralFragment() {
        mapRunner.execute()
    }

    private suspend fun runCoalition() {
        val drills = getBonusCoalitionDrills()

        val drillType = if (drills.size > 1) {
            drills.random()
        } else {
            drills.first()
        }

        val times = gameState.coalitionEnergy / 3
        if (times == 0) {
            updateCoalNextCheck()
            return
        }

        logger.info("Running Coalition Drill: $drillType $times times.")
        region.subRegion(781 + (440 * (drillType.ordinal - 1)), 855, 307, 110).click()
        delay((2500 * gameState.delayCoefficient).roundToLong())

        val capture = region.capture()
        if (Color(capture.getRGB(292, 706)).isSimilar(Color(156, 154, 156))) {
            logger.info("T dolls not assigned, using automatic assignment")
            region.subRegion(294, 792, 348, 91).click()
            delay(1000)
        }
        region.subRegion(1570, 845, 305, 108).click() // Attack
        region.waitHas(FileTemplate("ok.png"), 2000)?.click()
        logger.info("Starting drill, waiting for results screen")
        // wait for results, select run again if times > 1 else exit

        runSimCycles(times)
        logger.info("Completed all coalition drill")
        EventBus.publish(CoalitionEnergySpentEvent(drillType, times * 3, sessionId, elapsedTime))
        gameState.coalitionEnergy -= times * 3
        updateCoalNextCheck()
    }

    /**
     * Returns the bonus coalition drills, usually only 1 is open but may return all 3 if
     * we have all bonus event or its sunday
     */
    private fun getBonusCoalitionDrills(): List<Type> {
        val bonus = Color(247, 53, 66)
        val capture = region.capture()
        val list = mutableListOf<Type>()
        if (Color(capture.getRGB(1050, 410)).isSimilar(bonus)) list += Type.EXPDISKS
        if (Color(capture.getRGB(1492, 410)).isSimilar(bonus)) list += Type.PETRIDISH
        if (Color(capture.getRGB(1935, 410)).isSimilar(bonus)) list += Type.DATACHIPS
        return list
    }

    private suspend fun runSimCycles(times: Int) {
        var t = times
        while (coroutineContext.isActive) {
            region.subRegion(1250, 25, 710, 121).click() // endBattleClick
            delay(300)
            if (locations.getValue(LocationId.COMBAT_SIMULATION).isInRegion(region)) {
                break
            }
            if (region.subRegion(1100, 680, 275, 130).has(FileTemplate("ok.png"))) {
                if (--t == 0) {
                    region.subRegion(788, 695, 250, 96).click() // Cancel
                    break
                } else {
                    region.subRegion(1115, 695, 250, 96).click() // ok
                    logger.info("Done one cycle, remaining: $t")
                }
            }
        }
    }

    /**
     * Schedules the next check up time based on the required level passed in
     */
    private fun updateSimNextCheck(level: Level) {
        region.subRegion(400, 315, 185, 130).click()
        gameState.simNextCheck = Instant.now().plusSeconds(
            (
                (level.cost - gameState.simEnergy - 1)
                    * 7200) + simTimer.seconds
        )
        gameState.requiresUpdate
    }

    private fun updateCoalNextCheck() {
        region.findBest(FileTemplate("combat-simulation/coalition-drill.png"))?.region?.click()
        gameState.coalitionNextCheck = Instant.now().plusSeconds(
            (
                (2 - gameState.coalitionEnergy)
                    * 7200) + coalitionTimer.seconds
        )
        gameState.requiresUpdate
    }
}
