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

class EventDivision4_1(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventDivision4_1>()
    override val ammoResupplyThreshold = 0.9

    override suspend fun enterMap() {
        if (gameState.requiresMapInit) {
            DivisionUtils.panTopRight(this)
        }
        region.subRegion(540, 735, 67, 67).click()
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
            delay(750)
            logger.info("Pan up")
            val r = region.subRegionAs<AndroidRegion>(750, 420, 100, 300)
            r.swipeTo(r.copy(y = r.y - 200))
            delay((900 * gameState.delayCoefficient).roundToLong())
            gameState.requiresMapInit = false
        }

        deployEchelons(nodes[0], nodes[1], nodes[2])
        mapRunnerRegions.startOperation.click(); yield()
        resupplyEchelons(nodes[0])
        planPath()
        waitForTurnEnd(4, true)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting node ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Selecting node ${nodes[4]}")
        nodes[4].findRegion().click()

        logger.info("Selecting node ${nodes[5]}")
        nodes[5].findRegion().click(); yield()

        mapRunnerRegions.executePlan.click()
    }
}