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
import kotlin.math.roundToLong
import kotlin.random.Random

class EventDR_3_Inf(scriptComponent: ScriptComponent) :
    HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventDR_3_Inf>()

    override suspend fun enterMap() {
        if (scriptRunner.justRestarted) {
            delay(500)
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                15.0,
                500
            )

            logger.info("Pan to the right!")
            repeat(3) {
                region.subRegion(2100, 685, 50, 50)
                    .swipeTo(region.subRegion(400, 685, 50, 50), duration = 500)
                delay(300)
            }

            logger.info("Pan to the left!")
            region.subRegion(400, 685, 50, 50)
                .swipeTo(region.subRegion(2100, 685, 50, 50), duration = 500)
        }

        delay(2000)
        logger.info("Enter map ~~~")
        region.subRegion(720, 385, 1190, 155)
            .waitHas(FileTemplate("$PREFIX/entry.png", 0.8), 20_000)?.click()

        delay(1000)
        // Enter
        region.subRegion(1832, 590, 232, 110).click()
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                15.0,
                500
            )
            delay((900 * gameState.delayCoefficient).roundToLong()) //Wait to settle
            mapH = null
            gameState.requiresMapInit = false
        }
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])

        enterPlanningMode()
        logger.info("Selecting echelon at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()

        waitForTurnEnd(4)
        handleBattleResults()
    }
}