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
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventDivision2_1(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventDivision2_1>()

    override suspend fun enterMap() {
        val pad = Random.nextInt(0, 50)
        val r = region.subRegionAs<AndroidRegion>(250 + pad, 200, 1, 1)

        if (gameState.requiresMapInit) {
            DivisionUtils.panTopRight(this)
            repeat(2) {
                r.swipeTo(r.copy(x = r.x + 880))
                delay(500)
            }
            //DivisionUtils.panLeft(this, 1)
            region.subRegion(1304, 611, 30, 30).click() // hopefully map box is here
        } else {
            delay((1000 * gameState.delayCoefficient).roundToLong())
            region.subRegion(1466, 569, 30, 30).click()
        }
        delay((1800 * gameState.delayCoefficient).roundToLong())
        region.subRegion(1833, 590, 230, 110).click()
        delay(2000)
    }

    override suspend fun resetView() {
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(600, 700),
            Random.nextInt(150, 250),
            0.0,
            800
        )
        delay(500)

        logger.info("Pan down")
        val r = region.subRegionAs<AndroidRegion>(450, 900, 150, 151)
        r.swipeTo(r.copy(y = r.y - Random.nextInt(200, 250)), 800)
        delay(500)

        logger.info("Zoom in")
        region.pinch(
            Random.nextInt(350, 360),
            Random.nextInt(395, 405),
            0.0,
            500
        )
        delay((1000 * gameState.delayCoefficient).roundToLong())
        mapH = null
    }

    override suspend fun begin() {
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)

        if (gameState.requiresMapInit) {
            resetView()
            gameState.requiresMapInit = false
        }

        val rEchelons = deployEchelons(nodes[0], nodes[1])
        mapRunnerRegions.startOperation.click(); yield()

        // The objectives come still come up, close them instead of G&K splash
        region.subRegion(348, 156, 167, 91).clickWhile(period = 1000, timeout = 5000) {
            region.subRegion(370, 0, 220, 150)
                .doesntHave(FileTemplate("combat/battle/terminate.png"))
        }

        resupplyEchelons(rEchelons)
        planPath()
        waitForTurnEnd(4, false)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); delay(500)

        logger.info("Selecting Echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting node ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting node ${nodes[3]}")
        nodes[3].findRegion().click()

        mapRunnerRegions.executePlan.click()
    }
}