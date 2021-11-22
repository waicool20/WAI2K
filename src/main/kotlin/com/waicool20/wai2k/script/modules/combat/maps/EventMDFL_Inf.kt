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
import com.waicool20.wai2k.util.isSimilar
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.awt.Color
import kotlin.math.roundToLong
import kotlin.random.Random

abstract class EventMDFL_Inf(scriptComponent: ScriptComponent) :
    HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventMapRunner>()

    override suspend fun enterMap() {
        delay(3000) // Wait for the difficulty mode popup to settle
        val capture = region.capture()
        if (!Color(capture.getRGB(145, 960)).isSimilar(Color(238, 243, 238), 7.0)) {
            logger.info("Not on Normal mode, switching...")
            region.subRegion(115, 947, 174, 61).click()
        }

        repeat(3) {
            // Swipe from bottom left to top right
            region.subRegion(570, 885, 40, 40)
                .swipeTo(region.subRegion(1200, 440, 40, 40))
        }

        // Click on map pin
        region.subRegion(1374, 339, 50, 50).click()
        delay(500)

        // Click on map entry
        region.subRegion(1073, 514, 258, 29).click()
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
            region.pinch(
                Random.nextInt(300, 350),
                Random.nextInt(400, 450),
                15.0,
                500
            )
            // Swipe up
            val r1 = region.subRegion(605, 761, 50, 50)
            val r2 = r1.copy(y = r1.y - 400)
            r1.swipeTo(r2)
            gameState.requiresMapInit = false
        }
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])
        delay(500)
        enterPlanningMode()

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
        waitForTurnEnd(4)
        handleBattleResults()
    }
}