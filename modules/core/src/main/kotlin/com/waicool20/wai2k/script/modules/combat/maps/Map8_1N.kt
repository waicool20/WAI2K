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
import com.waicool20.wai2k.script.modules.combat.CorpseDragging
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.delay
import kotlin.math.roundToLong
import kotlin.random.Random

@Suppress("unused", "ClassName")
class Map8_1N(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    CorpseDragging {
    private val logger = loggerFor<Map8_1N>()

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            // All nodes will be on screen, only 'sticks' after a successful run
            logger.info("Zoom out")
            repeat(2) {
                region.pinch(
                    Random.nextInt(700, 800),
                    Random.nextInt(200, 300),
                    0.0,
                    1000
                )
                delay(500)
            }
            val r1 = region.subRegion(292, 266, 126, 137)
            val r2 = r1.copy().apply { translate(400, 200) }
            repeat(2) {
                r1.swipeTo(r2)
            }
            r2.swipeTo(r1)
            gameState.requiresMapInit = false
        }
        delay((500 * gameState.delayCoefficient).roundToLong())

        val rEchelons = deployEchelons(nodes[3], nodes[0])
        mapRunnerRegions.startOperation.click()
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
            enterPlanningMode()
            selectNodes(3, 2, 1)
            logger.info("Executing plan")
            mapRunnerRegions.executePlan.click()
            waitForTurnEnd(5, false)
            terminateMission()
        }
    }
}
