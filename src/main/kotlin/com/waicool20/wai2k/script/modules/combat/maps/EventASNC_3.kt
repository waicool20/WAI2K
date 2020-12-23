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


class EventASNC_3(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventASNC_3>()
    private val deadzone = region.subRegion(60, 420, 65, 65)

    override suspend fun enterMap() {
        region.subRegion(677, 610, 471, 235).click()
        delay(750)
        region.subRegion(1453, 842, 327, 112).click()
        delay(3000)
        region.waitHas(FileTemplate("combat/battle/start.png"), 15000)
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                0.0,
                1000
            )
            delay((900 * gameState.delayCoefficient).roundToLong()) //Wait to settle
            gameState.requiresMapInit = false
        }
        deployEchelons(nodes[0], nodes[1], nodes[2])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0], nodes[1], nodes[2])
        planPath()
        waitForTurnAndPoints(2, 1, timeout = 360_000)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Selecting echelon at command post")

        deadzone.click()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click(); delay(200)

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click(); delay(200)

        deadzone.click(); delay(200)

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click(); delay(200)

        logger.info("Selecting ${nodes[4]}")
        nodes[4].findRegion().click(); delay(200)

        deadzone.click(); delay(200)

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click(); delay(200)

        logger.info("Selecting ${nodes[5]}")
        nodes[5].findRegion().click(); delay(200)

        // ---

        deadzone.click(); delay(200)

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click(); delay(200)

        logger.info("Selecting ${nodes[9]}")
        nodes[9].findRegion().click(); delay(200)

        logger.info("Selecting ${nodes[6]}")
        nodes[6].findRegion().click(); delay(200)

        deadzone.click(); delay(200)

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click(); delay(200)

        logger.info("Selecting ${nodes[7]}")
        nodes[7].findRegion().click(); delay(200)

        deadzone.click(); delay(200)

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click(); delay(200)

        logger.info("Selecting ${nodes[8]}")
        nodes[8].findRegion().click(); delay(200)

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}