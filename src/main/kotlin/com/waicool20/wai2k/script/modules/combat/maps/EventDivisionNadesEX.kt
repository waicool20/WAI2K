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
import com.waicool20.wai2k.script.modules.combat.AbsoluteMapRunner
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventDivisionNadesEX(scriptComponent: ScriptComponent) : AbsoluteMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventDivisionNadesEX>()
    override val ammoResupplyThreshold = 0.6

    override suspend fun enterMap() {
        val r1 = region.subRegionAs<AndroidRegion>(1850, 315, 60, 60)
        val r2 = region.subRegionAs<AndroidRegion>(1194, 977, 60, 60)
        if (gameState.requiresMapInit) {
            delay(5000)
            DivisionUtils.setDifficulty(this)
            repeat(3) {
                r2.swipeTo(r1)
            }
            r1.swipeTo(r2)
            region.subRegion(1390, 304, 60, 60).click()
        }
        delay((1800 * gameState.delayCoefficient).roundToLong())
        region.subRegion(1833, 590, 230, 110).click()
        delay(3000)
        // maybe check full storage here
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)
    }

    override suspend fun begin() {
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(700, 800),
            Random.nextInt(100, 200),
            0.0,
            500
        )

        delay((1000 * gameState.delayCoefficient).roundToLong())
        gameState.requiresMapInit = false

        logger.info("Just gonna farm 404 grenades :/")
        for (i in 1..41) {
            // Deploy whatever
            deployEchelons(nodes[0])
            mapRunnerRegions.startOperation.click(); yield()
            delay(2000)

            // The objectives come still come up, close them instead of G&K splash
            region.subRegion(348, 156, 167, 91).clickWhile(period = 1000, timeout = 5000) {
                region.subRegion(370, 0, 220, 150)
                    .doesntHave(FileTemplate("combat/battle/terminate.png"))
            }

            // Plan dummy to reveal spot
            logger.info("Entering planning mode")
            mapRunnerRegions.planningMode.click(); delay(500)

            logger.info("Selecting node ${nodes[0]}")
            nodes[0].findRegion().click()

            logger.info("Selecting node ${nodes[1]}")
            nodes[1].findRegion().click(); yield()

            mapRunnerRegions.executePlan.click()

            waitForTurnAssets(listOf(FileTemplate("combat/battle/plan.png", 0.96)), false)
            delay(1000)

            // Plan dummy to frag grenades next turn
            // is stuff gonna block this?
            logger.info("Entering planning mode")
            mapRunnerRegions.planningMode.click(); delay(500)

            logger.info("Selecting node ${nodes[2]}")
            nodes[2].findRegion().click(); yield()

            mapRunnerRegions.executePlan.click()

            // Get the grenades
            waitForTurnAssets(listOf(FileTemplate("combat/battle/use-arrow.png")), false)
            delay(1000)

            logger.info("Looting grenades")
            region.subRegion(40, 730, 200, 70).click()
            delay(3000)

            // Close the item popup
            mapRunnerRegions.battleEndClick.clickWhile(period = 1000, timeout = 4000) {
                region.subRegion(370, 0, 220, 150)
                    .doesntHave(FileTemplate("combat/battle/terminate.png"))
            }
            delay((500 * gameState.delayCoefficient).roundToLong())
            restartMission()
        }
    }
}