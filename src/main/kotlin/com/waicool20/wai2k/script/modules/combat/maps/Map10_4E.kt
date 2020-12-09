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
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.AbsoluteMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class Map10_4E(scriptComponent: ScriptComponent) : AbsoluteMapRunner(scriptComponent) {
    private val logger = loggerFor<Map10_4E>()

    override suspend fun begin() {
        val r = region.subRegionAs<AndroidRegion>(1058, 700, 100, 3)
        if (gameState.requiresMapInit) {
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
            logger.info("Pan uo")
            repeat(2) {
                r.swipeTo(r.copy(y = r.y + 400))
                delay(200)
            }
            logger.info("Pan down")
            r.swipeTo(r.copy(y = r.y - 690))
            logger.info("Map hopefully aligned")
        }
        delay((900 * gameState.delayCoefficient).roundToLong()) //Wait to settle
        val rEchelons = deployEchelons(nodes[0], nodes[1], nodes[2])

        //Heavyports are configured now
        gameState.requiresMapInit = false
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()

        resupplyEchelons(rEchelons)
        planPath()
        waitForTurnEnd(5, false); delay(1000)

        waitForTurnAssets(listOf(FileTemplate("combat/battle/plan.png", 0.96)), false)
        delay(500)
        r.click()
        retreatEchelons(nodes[5])
        terminateMission()
    }

    private suspend fun planPath() {
        //randomize the route
        var ranNodes = listOf(3, 4)
        if (Random.nextBoolean()) {
            ranNodes = ranNodes.reversed()
        }

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting ${nodes[ranNodes[0]]}")
        nodes[ranNodes[0]].findRegion().click()

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting ${nodes[ranNodes[1]]}")
        nodes[ranNodes[1]].findRegion().click(); yield()

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}