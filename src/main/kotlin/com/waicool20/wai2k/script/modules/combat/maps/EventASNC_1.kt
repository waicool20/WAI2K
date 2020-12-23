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


class EventASNC_1(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventASNC_1>()
    override val ammoResupplyThreshold = 0.6

    override suspend fun enterMap() {
        region.subRegion(775, 240, 460, 226).click()
        delay((900 * gameState.delayCoefficient).roundToLong())
        region.subRegion(1454, 844, 327, 110).click()
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(800, 900),
                Random.nextInt(300, 400),
                0.0,
                800
            )
            delay(500)
            gameState.requiresMapInit = false
        }
        delay((900 * gameState.delayCoefficient).roundToLong())


        // Teams with only 1 doll can farm this so resupplies are never gonna be perfect
        deployEchelons(nodes[0], nodes[1], nodes[2])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()

        resupplyEchelons(nodes[0], nodes[1])
        planPath()

        waitForTurnEnd(4, false) // Ends when all are wiped
        handleBattleResults()
    }

    private suspend fun planPath() {
        val r = region.subRegion(80, 350, 250, 250) // Hopefully empty for deselecting

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); delay(500)

        r.click(); delay(500)

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting node ${nodes[3]}")
        nodes[3].findRegion().click(); yield()

        r.click(); delay(500)

        logger.info("Selecting echelon at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Selecting node ${nodes[4]}")
        nodes[4].findRegion().click(); yield()

        logger.info("Selecting node ${nodes[4]}")
        nodes[5].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}