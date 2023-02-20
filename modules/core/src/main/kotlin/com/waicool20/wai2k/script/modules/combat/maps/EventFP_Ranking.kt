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

class EventFP_Ranking(scriptComponent: ScriptComponent) :
    HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventFP_Inf>()

    override suspend fun enterMap() {
        FPUtils.enterRanking(this)

        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(900, 1000),
            Random.nextInt(300, 400),
            15.0,
            500
        )

        // Swipe up right
        val r1 = region.subRegion(500, 900, 200, 120)
        val r2 = r1.copy(x = r1.x + 200, y = r1.y - 200)
        logger.info("Swipe up right")
        r1.swipeTo(r2)

        // Click map entry
        logger.info("Click map pin")
        region.subRegion(600, 300, 280, 170).click()

        delay(500)

        // Confirm start
        logger.info("Confirm start")
        region.subRegion(1832, 590, 232, 112).click()
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
            gameState.requiresMapInit = false
        }
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        delay(8000)
        terminateMission()
    }
}
