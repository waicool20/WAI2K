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
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class Map12_4E(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    CorpseDragging {
    private val logger = loggerFor<Map12_4E>()
    override val ammoResupplyThreshold = 0.8
    override val rationsResupplyThreshold = 0.4

    override suspend fun resetView() {
        // Out to max zoom
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(800, 900),
            Random.nextInt(150, 250),
            0.0,
            800
        )
        delay((1000 * gameState.delayCoefficient).roundToLong()) //settle

        // In to tolerable zoom
        logger.info("Zoom in")
        region.pinch(
            Random.nextInt(360, 380),
            Random.nextInt(425, 445),
            0.0,
            500
        )
        delay((500 * gameState.delayCoefficient).roundToLong())

        // move area of interest closer to middle
        logger.info("Pan up")
        val r = region.subRegion(1058, 224, 100, 22)
        r.swipeTo(r.copy(y = r.y - 170))
        delay((500 * gameState.delayCoefficient).roundToLong())
        mapH = null
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            resetView()
        }

        // May the swaps faster soontm
        val rEchelons = deployEchelons(nodes[0], nodes[1])
        mapRunnerRegions.startOperation.click(); yield()
        gameState.requiresMapInit = false // Heavyports
        waitForGNKSplash()

        resupplyEchelons(rEchelons + nodes[1])
        delay((500 * gameState.delayCoefficient).roundToLong())
        planPath()

        // End turn automatically, save frames
        waitForTurnEnd(4, false)
        delay(3000)
        handleBattleResults()
    }

    private suspend fun planPath() {

        // Deselect echelon 2 to reduce getting them killed
        region.subRegion(151, 360, 72, 72).click()
        delay((300 * gameState.delayCoefficient).roundToLong())

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click(); yield()

        delay((500 * gameState.delayCoefficient).roundToLong())
        if (!checkPlaning(-1)) {
            mapH = null
            mapRunnerRegions.planningMode.click(); yield()
            planPath()
        }

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }

    private fun checkPlaning(targetPoints: Int): Boolean {
        // moved elsewhere hopefully never
        // checks if AP left over from planning is what it should be
        val actionPoints = Ocr.forConfig(config)
            .doOCRAndTrim(region.subRegion(1777, 979, 100, 62))
            .toIntOrNull()
        return if (actionPoints == targetPoints) {
            true
        } else {
            logger.info("Checking for remaining AP of $targetPoints, got $actionPoints instead")
            false
        }
    }
}