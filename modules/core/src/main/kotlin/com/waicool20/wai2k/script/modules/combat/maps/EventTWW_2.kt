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

package com.waicool20.wai2k.script.modules.combat.maps

import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

@Suppress("unused", "ClassName")
class EventTWW_2(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventTWW_2>()
    override val ammoResupplyThreshold = 0.6
    override val rationsResupplyThreshold = 0.6

    override suspend fun enterMap() {
        region.subRegion(1432, 335, 450, 222).click() // Wheel of Fortune
        delay((900 * gameState.delayCoefficient).roundToLong())
        region.subRegion(1455, 844, 320, 110).click() // Normal Battle
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            // Try get all nodes on screen
            logger.info("Zoom out")
            repeat(2) {
                region.pinch(
                    Random.nextInt(700, 800),
                    Random.nextInt(300, 400),
                    0.0,
                    500
                )
                delay(200)
            }
            delay(500)
            gameState.requiresMapInit = false
        }
        val rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        // Mission Objectives popup if this is too laggy dependant will need an asset test
        delay(500)
        region.subRegion(235, 142, 173, 79).click()
        resupplyEchelons(rEchelons)
        planPath()
        waitForTurnEnd(3)
        handleBattleResults()
    }

    private suspend fun planPath() {
        enterPlanningMode()

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click(); yield()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}
