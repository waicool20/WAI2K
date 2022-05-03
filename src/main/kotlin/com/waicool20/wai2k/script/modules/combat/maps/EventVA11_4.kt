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
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventVA11_4(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventVA11_4>()

    override suspend fun enterMap() {
        if (gameState.requiresMapInit) {
            val r1 = region.subRegion(1670, 350, 153, 500)
            val r2 = r1.copy(x = r1.x - 500)
            // Swipe to right
            repeat(2) {
                r1.swipeTo(r2)
            }

            delay(500)
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                15.0,
                500
            )
            delay(500)
        }

        // Click map
        region.subRegion(412, 366, 174, 29).click()
        delay(3000)
        // Enter battle
        region.subRegion(1832, 589, 232, 111).click()
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

            logger.info("Zoom in")
            region.pinch(
                Random.nextInt(300, 320),
                Random.nextInt(380, 400),
                15.0,
                500
            )
            logger.info("Move up")
            val r1 = region.subRegion(600, 290, 48, 48)
            val r2 = r1.copy(y = r1.y + 200)
            r1.swipeTo(r2)
            gameState.requiresMapInit = false
        }

        deployEchelons(nodes[0], nodes[1])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])
        enterPlanningMode()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()

        waitForTurnEnd(5)
        handleBattleResults()
    }
}
