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
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class CampaignDD_3_2(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    CampaignMapRunner {
    private val logger = loggerFor<CampaignDD_3_2>()

    override suspend fun enterMap() {
        CampaignUtils.selectCampaign(this)
        CampaignUtils.selectCampaignChapter(this)
        CampaignUtils.enterSimpleMap(this)
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
        resupplyEchelons(nodes[0])
        planPath()
        waitForTurnAndPoints(2, 2, false, 180_000)
        terminateMission()
    }

    private suspend fun planPath() {
        enterPlanningMode()

        logger.info("Selecting node: ${nodes[3]}")
        nodes[3].findRegion().click(); yield()

        logger.info("Selecting node: ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}
