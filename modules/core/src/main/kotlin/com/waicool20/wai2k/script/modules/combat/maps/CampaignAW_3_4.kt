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
import com.waicool20.wai2k.script.modules.combat.CampaignMapRunner
import com.waicool20.wai2k.script.modules.combat.AbsoluteMapRunner
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class CampaignAW_3_4(scriptComponent: ScriptComponent) : AbsoluteMapRunner(scriptComponent),
    CampaignMapRunner {
    private val logger = loggerFor<CampaignAW_3_4>()

    override suspend fun enterMap() {
        CampaignUtils.selectCampaign(this)
        CampaignUtils.selectCampaignChapter(this)
        CampaignUtils.enterSimpleMap(this)
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            // All nodes will be on screen, only 'sticks' after a successful run
            logger.info("Zoom out")
            repeat(2) {
                region.pinch(
                    Random.nextInt(700, 800),
                    Random.nextInt(200, 300),
                    0.0,
                    1000
                )
                delay(500)
            }
            logger.info("Pan up")
            val r = region.subRegion(1058, 224, 100, 22)
            r.swipeTo(r.copy(y = r.y + 600))
            delay(500)
            gameState.requiresMapInit = false
        }
        delay((500 * gameState.delayCoefficient).roundToLong())

        deployEchelons(nodes[0], nodes[1])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])
        nodes[2].findRegion().click(); yield()
        delay(1000)
        region.subRegion(235, 142, 173, 79).click()
        delay(1000)
        deployEchelons(nodes[0])
        resupplyEchelons(nodes[0])
        planPath2()
        waitForTurnAndPoints(2, 1, false, 180_000)
        delay(500)
        val r = region.subRegion(1058, 224, 100, 22)
        r.swipeTo(r.copy(y = r.y + 600))
        retreatEchelons(nodes[0])
        nodes[2].findRegion().click(); yield()
        nodes[0].findRegion().click(); yield()
        r.swipeTo(r.copy(y = r.y + 600))
        retreatEchelons(nodes[0])
        terminateMission()
    }

    private suspend fun planPath2() {
        enterPlanningMode()

        logger.info("Selecting node: ${nodes[3]}")
        nodes[3].findRegion().click(); yield()

        logger.info("Selecting node: ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}
