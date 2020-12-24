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


class EventASNC_2(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventASNC_2>()
    override val ammoResupplyThreshold = 0.6

    override suspend fun enterMap() {
        region.subRegion(1429, 329, 460, 226).click() // click ASNC map 2
        delay((900 * gameState.delayCoefficient).roundToLong())
        region.subRegion(1454, 844, 327, 110).click() // enter battle
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

        // turn 1
        val rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()
        resupplyEchelons(rEchelons)
        turn1() // move combat echelon to the right
        deployEchelons(nodes[0]) // deploy dummy, needs to be twice for some reason
        mapRunnerRegions.endBattle.click() // end turn
        waitForTurnAndPoints(2, 3, false)

        // turn 2
        waitForGNKSplash()
        swapEchelons(nodes[1] to nodes[0])
        planPath() // clear left node then go to command HQ
        waitForTurnAndPoints(2, 1, false) // turn does not end so wait for plan to be executed
        mapH = null // map moves after planned path, force rescan of map
        retreatEchelons(Retreat(nodes[0], true))
        terminateMission()
    }

    private suspend fun turn1() {
        logger.info("Moving combat echelon to ${nodes[1]}")
        nodes[1].findRegion().click(); yield()

        waitForTurnAndPoints(1, 1, false)

        // Deadzone
        region.subRegion(80, 350, 250, 250).click()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}