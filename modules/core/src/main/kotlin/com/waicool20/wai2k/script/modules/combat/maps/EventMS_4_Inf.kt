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

class EventMS_4_Inf(scriptComponent: ScriptComponent) :
    HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventMS_4_Inf>()

    override suspend fun enterMap() {
        if (gameState.requiresMapInit) {
            MSUtils.setDifficulty(this, MSUtils.Difficulty.NORMAL)
            MSUtils.enterChapter(this)
            MSUtils.panRight(this)
        }
        // Map selection sometimes zooms in for an unknown reason
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(900, 1000),
            Random.nextInt(300, 400),
            15.0,
            500
        )
        delay(500)

        // Click on map pin
        region.subRegion(1237, 513, 174, 36).click()

        // Enter
        delay((900 * gameState.delayCoefficient).roundToLong())
        region.subRegion(1832, 590, 232, 110).click()
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                15.0,
                500
            )
            delay((900 * gameState.delayCoefficient).roundToLong()) //Wait to settle

            logger.info("Pan down")
            val r = region.subRegion(400, 1000, 50, 50)
            r.swipeTo(r.copy(y = r.y - 300))
            delay((500 * gameState.delayCoefficient).roundToLong())
            gameState.requiresMapInit = false
        }
        val rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(rEchelons)

        enterPlanningMode()

        if (rEchelons.isEmpty()) {
            logger.info("Selecting echelon at ${nodes[0]}")
            nodes[0].findRegion().click()
        }

        logger.info("Selecting node at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Selecting node at ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()

        waitForTurnEnd(3)
        delay(5000)
        handleBattleResults()
    }
}
