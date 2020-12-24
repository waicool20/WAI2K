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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.AbsoluteMapRunner
import com.waicool20.wai2k.script.modules.combat.CorpseDragging
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class Map8_1N(scriptComponent: ScriptComponent) : AbsoluteMapRunner(scriptComponent), CorpseDragging {
    private val logger = loggerFor<Map8_1N>()

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            // All nodes will be on screen, only 'sticks' after a successful run
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(700, 800),
                Random.nextInt(200, 300),
                0.0,
                500)
            delay(500)
            logger.info("Pan up")
            val r = region.subRegionAs<AndroidRegion>(1058, 224, 100, 22)
            r.swipeTo(r.copy(y = r.y + 600))
            delay(500)
            gameState.requiresMapInit = false
        }
        delay((500 * gameState.delayCoefficient).roundToLong())

        nodes[1].findRegion()
        val rEchelons = deployEchelons(nodes[3], nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()

        resupplyEchelons(nodes[0])
        retreatEchelons(nodes[0])

        if (rEchelons.contains(nodes[3])) {
            // If a doll throws a smoke grenade may suicide the run
            logger.info("Dragging Echelon is NOT prepared to drag!")
            logger.info("Canceling this sortie")
            // Do over the map
            gameState.requiresMapInit = true
            terminateMission(incrementSorties = false)
        } else {
            // No suicide if Zas has correct stats
            planPath()
            waitForTurnEnd(5, false)
            terminateMission()
        }
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}