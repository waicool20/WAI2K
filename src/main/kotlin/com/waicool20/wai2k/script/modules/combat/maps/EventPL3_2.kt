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

class EventPL3_2(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventPL3_2>()
    override val ammoResupplyThreshold = 0.4
    override val rationsResupplyThreshold = 0.4

    override suspend fun enterMap() {
        PLUtils.enterChapter(this)

        if (gameState.requiresMapInit) {
            gameState.requiresMapInit = false
            val r1 = region.subRegion(770, 400, 60, 400)
            val r2 = r1.copy(x = r1.x + 700)

            repeat(3) { r2.swipeTo(r1) }
            repeat(2) { r1.swipeTo(r2) }

            region.subRegion(600, 610, 800, 50)
                .findBest(FileTemplate("$PREFIX/entrance.png"))?.region?.click()
        } else {
            region.subRegion(1163, 654, 150, 25).click()
        }

        delay(1000)
        region.subRegion(1832, 589, 232, 111).click() // Confirm start
        delay(1000)
    }

    override suspend fun begin() {

        // Turn 1
        val rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()
        resupplyEchelons(rEchelons)
        if (rEchelons.isEmpty()) {
            nodes[0].findRegion().click()
        }
        nodes[1].findRegion().click()
        delay(2000)

        // De-select current echelon
        region.subRegion(318, 417, 180, 180).click()
        deployEchelons(nodes[0])

        // Turn 1
        planPath()
        waitForTurnAndPoints(2, 1, false)
        mapH = null
        retreatEchelons(nodes[0])
        terminateMission()
    }

    private suspend fun planPath() {
        enterPlanningMode()

        logger.info("Selecting echelon at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Select ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Select ${nodes[0]}")
        nodes[0].findRegion().click()

        mapRunnerRegions.window.waitHas(FileTemplate("combat/battle/move.png"), 3000)?.click()
        delay(500)

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}