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
import kotlin.math.roundToInt
import kotlin.random.Random

class EventS_Ranking(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {

    private val logger = loggerFor<EventS_Ranking>()

    override suspend fun enterMap() {
        delay(3000)
        repeat(2) {
            region.copy(
                x = (region.width * 0.75).roundToInt(),
                width = region.width - (region.width * 0.75).roundToInt()
            ).pinch(
                Random.nextInt(300, 400),
                Random.nextInt(100, 200),
                90.0,
                500
            )
        }
        delay(1000)
        // Click hornets nest
        region.subRegion(990, 470, 40, 40).click(); delay(1000)
        // Click confirm
        region.subRegion(1759, 542, 232, 112).click()
    }

    override suspend fun begin() {
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(500, 600),
            Random.nextInt(300, 400),
            0.0,
            500
        )
        logger.info("Zoom in")
        region.pinch(
            Random.nextInt(300, 350),
            Random.nextInt(400, 450),
            0.0,
            500
        )
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        delay(1000)
        // Dismiss mission objective
        region.subRegion(35, 144, 375, 77).click()
        resupplyEchelons(nodes[0])

        val r = mapRunnerRegions.planningMode.copy().apply { grow(-50, 0) }
        r.swipeTo(r.copy(y = r.y - 500))
        delay(500)
        planPath()
        waitForTurnEnd(2, false)
        combatSettlement(false)
        handleBattleResults()
    }

    private suspend fun planPath() {
        enterPlanningMode()

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}
