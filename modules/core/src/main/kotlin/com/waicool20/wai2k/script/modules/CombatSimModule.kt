/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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
import com.waicool20.cvauto.core.template.FT
import com.waicool20.cvauto.core.util.isSimilar
import com.waicool20.wai2k.config.Wai2kProfile.CombatSimulation.Coalition.Type
import com.waicool20.wai2k.config.Wai2kProfile.CombatSimulation.Level
import com.waicool20.wai2k.events.CoalitionEnergySpentEvent
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.events.SimEnergySpentEvent
import com.waicool20.wai2k.game.Echelon
import com.waicool20.wai2k.game.location.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.ScriptException
import com.waicool20.wai2k.script.ScriptTimeOutException
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.wai2k.util.digitsOnly
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.wai2k.util.readText
import kotlinx.coroutines.*
import java.awt.Color
import java.time.*
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong
import kotlin.random.Random

@Suppress("unused")
class CombatSimModule(navigator: Navigator) : ScriptModule(navigator) {
    private val logger = loggerFor<CombatSimModule>()
    private val modeRegion = region.subRegion(276, 158, 200, 922)

    @Suppress("ClassName")
    class NeuralCloudCorridor_Advanced(
        private val level: Level, private val times: Int, scriptComponent: ScriptComponent
    ) : HomographyMapRunner(scriptComponent) {

        private val logger = loggerFor<NeuralCloudCorridor_Advanced>()
        override suspend fun begin() {
            // echelon to use for the run
            val echelon = Echelon(number = profile.combatSimulation.neuralEchelon)

            // Plan the route on the first run
            region.waitHas(FT("combat/battle/start.png"), 10000)
            delay(1000)

            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                0.0,
                500
            )
            delay(500)

            logger.info("Pan up")
            val r = region.subRegion(700, 140, 400, 100)
            r.swipeTo(r.copy(y = r.y + 400))
            delay(1000) // Wait to settle

            nodes[0].findRegion().click()
            region.waitHas(FT("ok.png"), 3000)
            if (!echelon.clickEchelon(this, 140)) {
                throw ScriptException("Could not deploy echelon $echelon")
            }
            delay(1000)
            try {
                withTimeout(12000) {
                    while (coroutineContext.isActive) {
                        mapRunnerRegions.startOperation.click(); yield()

                        region.waitHas(FT("ok.png"), 2000)?.click()
                            ?: continue
                        break
                    }
                }
            } catch (e: TimeoutCancellationException) {
                throw ScriptTimeOutException("Could not start neural sim", e)
            }

            waitForGNKSplash(7000) // Map background makes it hard to find
            enterPlanningMode(); delay(500)
            nodes[0].findRegion().click(); delay(500)
            nodes[1].findRegion().click(); delay(500)
            mapRunnerRegions.executePlan.click()
            waitForTurnEnd(1, timeout = 60_000)

            var runs = 0
            var energySpent = 0
            while (coroutineContext.isActive) {
                mapRunnerRegions.battleEndClick.click()
                delay(300)
                if (locations.getValue(LocationId.COMBAT_SIMULATION).isInRegion(region)) {
                    energySpent += level.cost
                    break
                }
                if (region.subRegion(982, 680, 275, 130).has(FT("ok.png"))) {
                    energySpent += level.cost
                    if (++runs > times) {
                        region.subRegion(668, 695, 250, 96).click() // Cancel
                        break
                    } else {
                        region.subRegion(996, 695, 250, 96).click() // ok
                        logger.info("Done one cycle, remaining: ${times - runs}")
                        delay(7000)
                        waitForTurnEnd(1, timeout = 60_000)
                    }
                }
            }
            logger.info("Completed all neural sim")
            EventBus.publish(
                SimEnergySpentEvent(
                    "NEURAL_FRAGMENT", level, energySpent, sessionId, elapsedTime
                )
            )
            gameState.simEnergy -= energySpent
        }
    }

    override suspend fun execute() {
        if (!profile.combatSimulation.enabled) return
        if (Instant.now() < gameState.simNextCheck) return
        executeNormal()
        executeCoalition()
    }

    private suspend fun executeNormal() {
        navigator.navigateTo(LocationId.COMBAT_SIMULATION)
        delay(1000) // Delay for settle

        // Select normal combat sim
        region.subRegion(274, 158, 200, 106).click()

        logger.info("Checking sim energy...")

        gameState.simEnergy = checkSimEnergy(region.subRegion(1414, 161, 71, 71)) ?: return

        for (run in listOf(::runDataSimulation, ::runNeuralFragment).shuffled()) {
            if (gameState.simEnergy <= 0) break
            run()
        }

        logger.info("Sim energy remaining : ${gameState.simEnergy}")
    }

    private suspend fun executeCoalition() {
        if (!profile.combatSimulation.coalition.enabled) return

        navigator.navigateTo(LocationId.COMBAT_SIMULATION)

        logger.info("Selecting coalition tab")
        while (coroutineContext.isActive) {
            delay(5000)
            modeRegion.findBest(FT("combat-simulation/coalition-drill.png"))?.region?.click()
            if (region.pickColor(1790, 205).isSimilar(Color(126, 24, 24))) {
                delay(1000)
                break
            }
        }

        logger.info("Checking coalition energy...")

        gameState.coalitionEnergy = checkSimEnergy(region.subRegion(1432, 161, 71, 71)) ?: return
        runCoalition()
        logger.info("Coalition energy remaining : ${gameState.coalitionEnergy}")
    }

    private suspend fun checkSimEnergy(
        energyRegion: AnyRegion, retries: Int = 5
    ): Int? {
        // Check the current sim energy and the duration until the next energy recharges
        var r = retries

        while (coroutineContext.isActive) {
            val energyString = ocr.digitsOnly().readText(energyRegion, threshold = 0.7)
            logger.debug("Sim energy OCR: $energyString")

            if (energyString.isBlank()) return 0

            val energy = energyString.toIntOrNull()
            if (energy == null || energy !in 0..12) {
                if (r-- > 0) continue else break
            }
            return energy
        }
        logger.debug("Sim energy unreadable!")
        return null
    }

    private suspend fun runDataSimulation() {
        if (profile.combatSimulation.dataSim == Level.OFF) return

        val level = profile.combatSimulation.dataSim
        val times = gameState.simEnergy / level.cost
        if (times == 0) {
            updateNextCheck()
            return
        }

        modeRegion.findBest(FT("combat-simulation/data-mode.png", 0.8))?.region?.click()
        // Generous Delays here since combat sims don't occur often
        delay((1000 * gameState.delayCoefficient).roundToLong())

        logger.info("Running data sim type $level $times times")
        region.subRegion(617, 377 + (177 * (level.cost - 1)), 1230, 130).click() // Difficulty
        delay(1000)
        logger.info("Entering $level sim")
        region.subRegion(1194, 810, 300, 105).click() // Enter Combat
        region.waitHas(FT("ok.png"), 5000)?.click()
        delay(3000)

        logger.info("Clicking through sim results")

        runSimCycles(times)
        logger.info("Completed all data sim")
        EventBus.publish(
            SimEnergySpentEvent(
                "DATA_SIMULATION", level, times * level.cost, sessionId, elapsedTime
            )
        )
        gameState.simEnergy -= times * level.cost
        updateNextCheck()
    }

    private suspend fun runNeuralFragment() {
        if (profile.combatSimulation.neuralFragment == Level.OFF) return

        val level = profile.combatSimulation.neuralFragment
        val times = gameState.simEnergy / level.cost
        if (times == 0) {
            updateNextCheck()
            return
        }

        modeRegion.findBest(FT("combat-simulation/neural.png", 0.8))?.region?.click()
        delay((1000 * gameState.delayCoefficient).roundToLong())

        logger.info("Running neural sim type $level $times times")
        logger.info("Entering $level sim")
        region.subRegion(617, 377 + (177 * (level.cost - 1)), 1230, 130).click() // Difficulty

        NeuralCloudCorridor_Advanced(level, times, this@CombatSimModule).execute()
        updateNextCheck()
    }

    private suspend fun runCoalition() {
        val times = gameState.coalitionEnergy / 3
        if (times == 0) {
            updateNextCheck()
            return
        }

        val drills = getBonusCoalitionDrills()

        val drillType = when (drills.size) {
            0 -> {
                // Bonus detection failed, just use preferred type as fallback
                logger.warn("Could not detect which coalition drills have bonuses")
                if (profile.combatSimulation.coalition.preferredType == Type.RANDOM) {
                    listOf(Type.EXPDISKS, Type.PETRIDISH, Type.DATACHIPS).random()
                } else {
                    profile.combatSimulation.coalition.preferredType
                }
            }
            1 -> drills.first()
            else -> {
                // Use preferred type if its on bonus otherwise do a random one
                if (profile.combatSimulation.coalition.preferredType in drills) {
                    profile.combatSimulation.coalition.preferredType
                } else drills.random()
            }
        }

        logger.info("Running Coalition Drill: $drillType $times times.")
        region.subRegion(617 + (440 * (drillType.ordinal - 1)), 855, 307, 110).click()
        delay((2500 * gameState.delayCoefficient).roundToLong())

        if (region.pickColor(254, 688).isSimilar(Color(156, 154, 156))) {
            logger.info("T-Dolls not assigned, using automatic assignment")
            region.subRegion(176, 792, 348, 91).click()
            delay(1000)
        }
        region.subRegion(1451, 845, 305, 108).click() // Attack
        region.waitHas(FT("ok.png"), 2000)?.click()
        logger.info("Starting drill, waiting for results screen")
        // wait for results, select run again if times > 1 else exit

        runSimCycles(times)
        logger.info("Completed all coalition drill")
        EventBus.publish(CoalitionEnergySpentEvent(drillType, times * 3, sessionId, elapsedTime))
        gameState.coalitionEnergy -= times * 3
        updateNextCheck()
    }

    /**
     * Returns the bonus coalition drills, usually only 1 is open but may return all 3 if
     * we have all bonus event, or it's sunday
     */
    private fun getBonusCoalitionDrills(): List<Type> {
        val bonus = Color(244, 54, 65)
        val capture = region.freeze()
        val list = mutableListOf<Type>()
        if (capture.pickColor(936, 410).isSimilar(bonus)) list += Type.EXPDISKS
        if (capture.pickColor(1373, 410).isSimilar(bonus)) list += Type.PETRIDISH
        if (capture.pickColor(1814, 410).isSimilar(bonus)) list += Type.DATACHIPS
        return list
    }

    private suspend fun runSimCycles(times: Int) {
        var t = times
        while (coroutineContext.isActive) {
            region.subRegion(1250, 25, 600, 121).click() // endBattleClick
            delay(300)
            if (locations.getValue(LocationId.COMBAT_SIMULATION).isInRegion(region)) {
                break
            }
            if (region.subRegion(983, 680, 275, 130).has(FT("ok.png"))) {
                if (--t > 0) {
                    region.subRegion(996, 695, 250, 96).click() // ok
                    logger.info("Done one cycle, remaining: $t")
                } else {
                    region.subRegion(788, 695, 250, 96).click() // Cancel
                }
            }
        }
    }

    /**
     * Schedules the next check-up time
     */
    private fun updateNextCheck() {
        gameState.simNextCheck = OffsetDateTime.of(
            LocalDate.now(), LocalTime.of(0, 0), ZoneOffset.ofHours(-8)
        ).plusDays(1).toInstant()
    }
}
