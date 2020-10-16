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
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventGSGNewFork(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent), EventMapRunner {
    private val logger = loggerFor<EventGSGNewFork>()
    override val isCorpseDraggingMap = false

    override val rationsResupplyThreshold = 0.5

    override suspend fun enterMap() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                0.0,
                1000)
            delay((900 * gameState.delayCoefficient).roundToLong()) //Wait to settle
        }
        logger.info("Pan down")
        val r = region.subRegionAs<AndroidRegion>(230, 178, 520, 150)
        r.swipeTo(r.copy(y = r.y + 450), 500)
        logger.info("Entering map")
        region.subRegion(791, 450, 141, 27).click() // New Fork
        delay(2000)
        region.subRegion(1833, 590, 230, 109).click() // Start Mission
        delay(1000)
        region.waitHas(FileTemplate("combat/battle/start.png"), 10000)
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                0.0,
                1000)
            delay((900 * gameState.delayCoefficient).roundToLong()) //Wait to settle
            gameState.requiresMapInit = false
        }
        // Deploy the dummy
        deployEchelons(2 at nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()

        moveDummy() // Map will pan when switching with Angelica
        waitForTurnAssets(false, 0.96, "combat/battle/plan.png")

        mapH = null
        deployEchelons(nodes[0])
        delay(1000)
        resupplyEchelons(nodes[0])

        planPath()
        waitForTurnEnd(7, false) // Ends automatically at turn 3
        handleBattleResults()
    }

    private suspend fun moveDummy() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting dummy at command post")
        nodes[0].findRegion().click()

        logger.info("Moving them to rally point")
        nodes[1].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at command post")
        nodes[0].findRegion().click()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Selecting ${nodes[4]}")
        nodes[4].findRegion().click()

        logger.info("Selecting ${nodes[5]}")
        nodes[5].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}