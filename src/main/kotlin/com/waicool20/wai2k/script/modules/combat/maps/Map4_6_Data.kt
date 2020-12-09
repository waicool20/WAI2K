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

package com.waicool20.wai2k.script.modules.combat.maps

import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class Map4_6_Data(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent) {
    private val logger = loggerFor<Map4_6>()

    // Maybe have this as an option for regular 4-6
    //Allow interruption of waiting for turn if necessary
    private var combatComplete = false

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            repeat(2) {
                region.pinch(
                    Random.nextInt(700, 800),
                    Random.nextInt(300, 400),
                    0.0,
                    500
                )
                delay(500)
            }
            delay((900 * gameState.delayCoefficient).roundToLong())
            gameState.requiresMapInit = false
        }

        // Will probably get get stuck if ? nodes reduce you manpower to 0
        deployEchelons(nodes[0], nodes[1])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        planPath()

        // If you get ambushed on the final ? node waitForTurnAndPoints() can be satisfied
        // As the "You have been ambushed popup is there with a delay to get into battle
        waitForTurnAssets(listOf(FileTemplate("combat/battle/plan.png", 0.96)), false)
        if (interruptWaitFlag) {
            while (!combatComplete) delay(1000)
        }

        interruptWaitFlag = false
        terminateMission()
    }

    override suspend fun onEnterBattleListener() {
        interruptWaitFlag = true
        logger.info("Postmortem: battle detected")
        mapRunnerRegions.pauseButton.click()
        delay(1000)
        mapRunnerRegions.retreatCombat.click()
    }

    override suspend fun onFinishBattleListener() {
        combatComplete = true
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        if (Random.nextBoolean()) {
            logger.info("Selecting ${nodes[3]}")
            nodes[3].findRegion().click(); yield()

            logger.info("Selecting ${nodes[4]}")
            nodes[4].findRegion().click(); yield()
        } else {
            logger.info("Selecting ${nodes[4]}")
            nodes[4].findRegion().click(); yield()

            logger.info("Selecting ${nodes[3]}")
            nodes[3].findRegion().click(); yield()
        }

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}