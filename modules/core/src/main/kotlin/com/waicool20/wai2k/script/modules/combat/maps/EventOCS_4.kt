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

class EventOCS_4(scriptComponent: ScriptComponent) :
    HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventOCS_4>()

    override suspend fun enterMap() {

        if (gameState.requiresMapInit) {
            logger.info("Waiting for animation...")
            delay(1000)

        }
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(900, 1000),
            Random.nextInt(300, 400),
            15.0,
            500
        )
        delay(500)

        // Pan
        region.subRegion(1400, 165, 50, 50)
            .swipeTo(region.subRegion(400, 165, 50, 50))
        delay(500)
        // Click on map pin
        region.subRegion(1474, 696, 102, 26).click()
        delay(500)

        // Enter
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
            mapH = null
        }
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        if (gameState.requiresMapInit) {
            delay((1500 * gameState.delayCoefficient).roundToLong())
            logger.info("Exiting objectives popup")
            region.subRegion(50, 147, 240, 70).click()
            delay(1000)
            gameState.requiresMapInit = false
        }
        resupplyEchelons(nodes[0])
        delay(500)
        enterPlanningMode()

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
        waitForTurnEnd(5)
        handleBattleResults()
    }
}
