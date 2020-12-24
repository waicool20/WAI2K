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
import com.waicool20.wai2k.script.modules.combat.AbsoluteMapRunner
import com.waicool20.wai2k.script.modules.combat.CorpseDragging
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class Map0_2_EX(scriptComponent: ScriptComponent) : AbsoluteMapRunner(scriptComponent), CorpseDragging {
    private val logger = loggerFor<Map0_2_EX>()

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            // Check to see if its already good
            logger.info("Checking map zoom")
            if (region.subRegion(1327, 320, 146, 27)
                    .waitHas(FileTemplate("$PREFIX/zoom-anchor.png"), 5000) != null) {
                logger.info("zoom anchor found")
            } else {
                // Otherwise set it ourself
                // Bounce back for all nodes mostly on screen at once
                logger.info("Zoom out")
                region.pinch(
                    Random.nextInt(900, 1000),
                    Random.nextInt(300, 400),
                    0.0,
                    1000)
            }
            gameState.requiresMapInit = false
        }

        delay((900 * gameState.delayCoefficient).roundToLong()) //Wait to settle
        val rEchelons = deployEchelons(nodes[0], nodes[1])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(rEchelons + nodes[1])
        planPath()
        waitForTurnEnd(5)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}
