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

@file:Suppress("unused", "ClassName")

package com.waicool20.wai2k.script.modules.combat.maps

import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventLB_Inf_Lewis(scriptComponent: ScriptComponent) : EventLB_Inf(scriptComponent)
class EventLB_Inf_P22(scriptComponent: ScriptComponent) : EventLB_Inf(scriptComponent)

sealed class EventLB_Inf(scriptComponent: ScriptComponent) :
    HomographyMapRunner(scriptComponent), EventMapRunner {
    private val logger = loggerFor<EventLB_Inf>()

    override suspend fun enterMap() {
        logger.info("Pinch out")
        region.pinch(
            Random.nextInt(900, 1000),
            Random.nextInt(300, 400),
            15.0,
            500
        )

        val r1 = region.subRegion(1564, 282, 90, 90)
        val r2 = r1.copy(r1.x - 500, r1.y + 500)
        r1.swipeTo(r2)
        delay(500)

        // Map pin
        logger.info("Click map pin")
        region.subRegion(1528, 685, 132, 36).click()
        delay(1000)

        // Confirm
        logger.info("Confirm")
        region.subRegion(1832, 589, 237, 120).click()
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
        resupplyEchelons(nodes[0])
        delay(500)
        enterPlanningMode()

        selectNodes(1)

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
        waitForTurnEnd(5)
        handleBattleResults()
    }
}
