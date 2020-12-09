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
import com.waicool20.wai2k.script.modules.combat.AbsoluteMapRunner
import com.waicool20.wai2k.script.modules.combat.CorpseDragging
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong

class Map11_5(scriptComponent: ScriptComponent) : AbsoluteMapRunner(scriptComponent), CorpseDragging {
    private val logger = loggerFor<Map11_5>()

    override suspend fun begin() {
        // No need to zoom, delay for map lag
        delay((1000 * gameState.delayCoefficient).roundToLong())
        val rEchelons = deployEchelons(nodes[1], nodes[0])
        // Dummy do not supply
        deployEchelons(nodes[2])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(rEchelons + nodes[0])
        retreatEchelons(nodes[0])
        planPath()
        // Wait for team to move all the way
        waitForTurnEnd(5, false); delay(1000)
        waitForTurnAndPoints(1, 0, false); delay(1000)
        retreatEchelons(nodes[0])
        terminateMission()
    }

    private suspend fun planPath() {

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }

}