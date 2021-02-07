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

class EventDivision5_1(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventDivision5_1>()
    override val ammoResupplyThreshold = 0.6

    override suspend fun enterMap() {
        if (gameState.requiresMapInit) {
            DivisionUtils.panTopRight(this)
            region.subRegion(933, 548, 65, 65).click()
        } else {
            region.subRegion(1024, 537, 64, 64).click()
        }
        delay((1800 * gameState.delayCoefficient).roundToLong())
        region.subRegion(1833, 590, 230, 110).click()
        delay(2000)
    }

    override suspend fun begin() {
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)

        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(800, 900),
                Random.nextInt(300, 400),
                0.0,
                500
            )
            delay(500)
            logger.info("Pan down")
            val r = region.subRegionAs<AndroidRegion>(450, 900, 150, 151)
            r.swipeTo(r.copy(y = r.y - 200), 500)
            delay((900 * gameState.delayCoefficient).roundToLong())
            gameState.requiresMapInit = false
        }

        val rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()

        // The objectives come still come up, close them instead of G&K splash
        region.subRegion(348, 156, 167, 91).clickWhile(period = 1000, timeout = 5000) {
            region.subRegion(370, 0, 220, 150)
                .doesntHave(FileTemplate("combat/battle/terminate.png"))
        }

        resupplyEchelons(rEchelons)
        planPath()
        waitForTurnEnd(3)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); delay(500)

        logger.info("Selecting Echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting node ${nodes[5]}")
        nodes[5].findRegion().click(); yield()

        mapRunnerRegions.executePlan.click()
    }
}