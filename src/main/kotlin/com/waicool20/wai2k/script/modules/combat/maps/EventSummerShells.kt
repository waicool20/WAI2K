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
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventSummerShells(
    scriptRunner: ScriptRunner,
    region: AndroidRegion,
    config: Wai2KConfig,
    profile: Wai2KProfile
) : EventMapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<EventSummerShells>()
    override val isCorpseDraggingMap = false

    // For panning and deselecting
    private val emptySpace = region.subRegionAs<AndroidRegion>(270, 700, 400, 140)

    override suspend fun enterMap() {
        region.subRegion(1445, 348, 450, 210).click(); delay(500) // Wheel Of Fortune
        region.subRegion(975, 830, 210, 90).click(); delay(3000) // Normal Battle
    }

    override suspend fun execute() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                0.0,
                500
            )
            delay((1000 * gameState.delayCoefficient).roundToLong()) // Wait to settle
            gameState.requiresMapInit = false
        }
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])

        turn1() // Crab may be here
        waitForTurnAndPoints(1, 1, false); delay(1000)

        // If there was no battle Echelon 1 will still be selected
        emptySpace.click(); delay(500) //Deselect them
        deployEchelons(nodes[0]); delay(500) // Dummy

        pathToNode()
        waitForTurnAndPoints(3, 3, false) // The crabs capture the heliport
        waitForTurnAssets(true, 0.96, "combat/battle/plan.png") // If melons on final node
        handleBattleResults()
    }

    private suspend fun turn1() {
        // Do this as a plan in case post battle clicks do not clear the results screen
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click(); delay(2000)
    }

    private suspend fun pathToNode() {
        // Perhaps figure out a way to get more checks or less battles
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Panning Down")
        emptySpace.swipeTo(emptySpace.copy(y = emptySpace.y - 580)); delay(500)

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click(); yield()
    }
}