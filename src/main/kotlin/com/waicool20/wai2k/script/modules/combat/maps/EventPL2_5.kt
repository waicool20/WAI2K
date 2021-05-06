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

class EventPL2_5(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventPL2_5>()

    override suspend fun enterMap() {
        PLUtils.enterChapter(this)

        if (gameState.requiresMapInit) {
            gameState.requiresMapInit = false
            val r1 = region.subRegion(770, 400, 60, 400)
            val r2 = r1.copy(x = r1.x + 700)

            repeat(3) { r2.swipeTo(r1) }

            region.subRegion(550, 550, 1000, 100)
                .findBest(FileTemplate("$PREFIX/entrance.png"))?.region?.click()
        } else {
            region.subRegion(960, 580, 140, 25).click()
        }

        delay(1000)
        region.subRegion(1832, 589, 232, 111).click() // Confirm start
        delay(1000)
    }

    override suspend fun begin() {
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(800, 900),
            Random.nextInt(250, 350),
            0.0,
            500
        )
        logger.info("Zoom in")
        region.pinch(
            Random.nextInt(360, 380),
            Random.nextInt(445, 475),
            0.0,
            500
        )
        delay((1000 * gameState.delayCoefficient).roundToLong())

        logger.info("Pan up")
        val r = region.subRegion(1058, 224, 100, 22)
        r.swipeTo(r.copy(y = r.y - 170))

        // Turn 1
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])
        nodes[1].findRegion().click()
        delay(2000)
        // De-select current echelon
        region.subRegion(318, 417, 180, 180).click()
        deployEchelons(nodes[0])
        // Since this isnt planned if post battle clicks borks
        // The script will timeout on battle results, sad face
        logger.info("Ending Turn 1")
        mapRunnerRegions.executePlan.click()

        waitForTurnEnd(1, false)
        // Turn 2
        waitForGNKSplash()
        mapH = null
        logger.info("Zoom in")
        region.pinch(
            Random.nextInt(390, 400),
            Random.nextInt(430, 440),
            0.0,
            500
        )

        planPath()

        waitForTurnAndPoints(3, 2, false)
        mapH = null
        retreatEchelons(nodes[0])
        terminateMission()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Select ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Select ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Select ${nodes[0]}")
        nodes[0].findRegion().click()

        mapRunnerRegions.window.waitHas(FileTemplate("combat/battle/move.png"), 3000)?.click()
        delay(800)

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}