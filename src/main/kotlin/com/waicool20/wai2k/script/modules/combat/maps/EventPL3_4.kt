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

import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlin.math.roundToLong
import kotlin.random.Random

class EventPL3_4(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventPL3_4>()
    override val ammoResupplyThreshold = 0.2
    override val rationsResupplyThreshold = 0.1

    override suspend fun enterMap() {
        PLUtils.enterChapter(this)
        if (gameState.requiresMapInit) {
            val r1 = region.subRegion(770, 400, 60, 400)
            val r2 = r1.copy(x = r1.x + 700)

            repeat(3) { r2.swipeTo(r1) }

            region.subRegion(687, 550, 120, 25).click()
        } else {
            region.subRegion(664, 555, 105, 30).click()
        }
        delay(1000)
        region.subRegion(1832, 589, 232, 111).click() // Confirm start
        delay(1000)
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            repeat(2) {
                region.pinch(
                    Random.nextInt(800, 900),
                    Random.nextInt(250, 350),
                    0.0,
                    500
                )
            }
            delay((1000 * gameState.delayCoefficient).roundToLong())
            gameState.requiresMapInit = false
        }

        // Turn 1
        val rEchelons = deployEchelons(nodes[0])
        deployEchelons(nodes[1])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()

        resupplyEchelons(rEchelons)
        planPath()
        waitForTurnAndPoints(1, 0, false)
        mapH = null
        retreatEchelons(nodes[0])
        terminateMission()
    }

    private suspend fun planPath() {
        enterPlanningMode()

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Select ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Select ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}