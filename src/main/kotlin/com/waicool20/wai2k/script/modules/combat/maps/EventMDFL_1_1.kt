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

import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.awt.Color
import kotlin.random.Random

class EventMDFL_1_1(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventMDFL_1_1>()

    override suspend fun enterMap() {
        // Attempt to align 3D mission select map for first entry
        if (gameState.requiresMapInit) {
            val difficultyImage = region.subRegion(165, 987, 3, 1).capture()

            // Check the colours of 2 pixels on the difficulty button to make sure we are on EZ mode
            if (difficultyImage.getRGB(1, 1) == Color(239, 235, 239).rgb
                && difficultyImage.getRGB(3, 1) == Color(49, 48, 49).rgb
            ) {
                logger.info("Normal Difficulty is ON")
            } else {
                // Otherwise press the button
                logger.info("Selecting normal difficulty")
                region.subRegion(110, 950, 170, 50).click()
                delay(1000)
            }
            // Scroll from bottom left to top right a couple times, it worked before
            logger.info("Scrolling map down and left")
            repeat(3) {
                region.subRegion(410, 920, 100, 100)
                    .swipeTo(region.subRegion(1400, 170, 100, 100), 500)
                delay(500)
            }

            // Click the map pin for chapter 1
            region.subRegion(1006, 367, 50, 50).click()
            delay(500)
            gameState.requiresMapInit = false
        } else {
            // Map should be here after a successful run
            // Click the map pin for chapter 1
            region.subRegion(985, 726, 50, 50).click()
            delay(500)
        }

        // Enter the witch button
        region.subRegion(1000, 670, 240, 40).click()
        delay(500)
        // Confirm start
        region.subRegion(1836, 592, 226, 104).click()
        delay(1000)
    }

    override suspend fun begin() {
        if (scriptRunner.justRestarted) {
            logger.info("Zooming out then in")
            region.pinch(
                Random.nextInt(700, 800),
                Random.nextInt(300, 400),
                0.0,
                500
            )
            delay(2000)
            region.pinch(
                Random.nextInt(300, 400),
                Random.nextInt(400, 500),
                0.0,
                500
            )
        }
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()
        enterPlanningMode()

        // Plan mode AI character to glowing node + 1 so turn auto ends
        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click(); yield()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click(); yield()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()

        waitForTurnEnd(2)
        handleBattleResults()
    }
}
