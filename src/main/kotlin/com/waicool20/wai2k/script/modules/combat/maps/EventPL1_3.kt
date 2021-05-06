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

import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventPL1_3(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventPL1_3>()

    override suspend fun enterMap() {
        PLUtils.enterChapter(this)
        if (gameState.requiresMapInit) {
            gameState.requiresMapInit = false
            val r1 = region.subRegion(770, 400, 60, 400)
            val r2 = r1.copy(x = r1.x + 700)

            repeat(4) { r1.swipeTo(r2) }
            r2.swipeTo(r1)

            region.subRegion(1110, 685, 950, 210)
                .findBest(FileTemplate("$PREFIX/entrance.png", 0.98))?.region?.click()
        } else {
            region.subRegion(1664, 800, 180, 35).click()
        }

        delay(1000)
        region.subRegion(1832, 589, 232, 111).click()
    }

    override suspend fun begin() {
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(800, 900),
            Random.nextInt(250, 350),
            0.0,
            500
        )
        delay((1000 * gameState.delayCoefficient).roundToLong())

        logger.info("Pan up")
        val r = region.subRegion(1058, 224, 100, 22)
        r.swipeTo(r.copy(y = r.y - 200))

        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])

        nodes[1].findRegion().click()
        delay(2000)
        // De-select current echelon
        region.subRegion(318, 417, 180, 180).click()
        deployEchelons(nodes[0])
        planPath()

        waitForTurnAndPoints(3, 0)
        delay(1000)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Select ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}