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

import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

@Suppress("unused", "ClassName")
class EventTPSM_Inf(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventTPSM_Inf>()

    override suspend fun enterMap() {
        withTimeout(7000) {
            while (isActive) {
                val entrance = region.waitHas(FileTemplate("$PREFIX/map-entrance.png", 0.9), 1000)
                if (entrance != null) {
                    entrance.click()
                    break
                } else {
                    region.pinch(
                        Random.nextInt(800, 900),
                        Random.nextInt(300, 400),
                        0.0,
                        500
                    )
                    repeat(2) {
                        val r = region.subRegion(150, 250, 200, 50)
                        r.swipeTo(r.copy(x = r.x + 1000), 500)
                    }
                    region.subRegion(1491, 419, 86, 25).click()
                    break
                }
            }
        }
        delay((1000 * gameState.delayCoefficient).roundToLong())
        region.subRegion(1832, 590, 230, 108).click() // Confirm Start Button
    }

    override suspend fun resetView() {
        // Map not at full zoom because small nodes were causing problems

        // Out to max zoom
        logger.info("Zoom out")
        repeat(2) {
            region.pinch(
                Random.nextInt(800, 900),
                Random.nextInt(300, 400),
                0.0,
                500
            )
            delay((500 * gameState.delayCoefficient).roundToLong())
        }

        // In to target zoom
        logger.info("Zoom in")
        region.pinch(
            Random.nextInt(360, 380),
            Random.nextInt(415, 430),
            0.0,
            500
        )
        delay((500 * gameState.delayCoefficient).roundToLong())

        // move area of interest closer to middle
        logger.info("Pan up")
        val r = region.subRegion(1058, 224, 100, 22)
        r.swipeTo(r.copy(y = r.y + 400))
        delay((500 * gameState.delayCoefficient).roundToLong())
        mapH = null
    }


    override suspend fun begin() {
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)

        if (gameState.requiresMapInit) {
            resetView()
        }

        val rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()

        // Close objectives popup
        if (gameState.requiresMapInit) {
            region.subRegion(350, 160, 160, 85).click()
        }
        gameState.requiresMapInit = false

        resupplyEchelons(rEchelons)
        planPath()
        waitForTurnEnd(5, false)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click()

        logger.info("Selecting Echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting node ${nodes[4]}")
        nodes[4].findRegion().click()

        logger.info("Selecting Echelon at ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        mapRunnerRegions.executePlan.click()
    }
}
