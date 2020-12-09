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
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class Map4_4N(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent) {
    private val logger = loggerFor<Map4_4N>()

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            repeat(2) {
                region.pinch(
                    Random.nextInt(900, 1000),
                    Random.nextInt(300, 400),
                    0.0,
                    1000)
            }
            delay((900 * gameState.delayCoefficient).roundToLong()) //Wait to settle
            gameState.requiresMapInit = false
        }

        deployEchelons(nodes[0], nodes[1])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])
        planPath()
        waitForTurnEnd(4)
        terminateMission()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}
