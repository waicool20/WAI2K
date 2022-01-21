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
import com.waicool20.wai2k.script.modules.combat.CorpseDragging
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class Map13_4(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    CorpseDragging {
    private val logger = loggerFor<Map13_4>()
    override val ammoResupplyThreshold = 0.8
    override val rationsResupplyThreshold = 0.4

    override suspend fun resetView() {
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(800, 900),
            Random.nextInt(150, 250),
            0.0,
            800
        )
        delay((1000 * gameState.delayCoefficient).roundToLong())
        mapH = null
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            resetView()
        }

        val rEchelons = deployEchelons(nodes[0], nodes[1])
        mapRunnerRegions.startOperation.click(); yield()
        gameState.requiresMapInit = false
        waitForGNKSplash()

        resupplyEchelons(rEchelons + nodes[1])
        delay((500 * gameState.delayCoefficient).roundToLong())
        planPath()

        // End turn automatically, save frames
        waitForTurnEnd(5, false)
        delay(3000)
        handleBattleResults()
    }

    private suspend fun planPath() {
        // Deselect echelon 2 to reduce getting them killed
        region.subRegion(151, 360, 72, 72).click()
        delay((300 * gameState.delayCoefficient).roundToLong())

        enterPlanningMode()

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}