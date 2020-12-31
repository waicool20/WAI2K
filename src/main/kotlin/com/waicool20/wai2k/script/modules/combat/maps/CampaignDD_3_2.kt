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

import ai.djl.modality.cv.ImageFactory
import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.modules.combat.CampaignMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.wai2k.util.ai.GFLObject
import com.waicool20.wai2k.util.ai.ModelLoader
import com.waicool20.wai2k.util.ai.YoloTranslator
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
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)
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
        retreatEchelons(Retreat(nodes[1], false))
        resupplyEchelons(nodes[0])
        planPath()
        waitForTurnAndPoints(2, 2, false)

        // alright lets do this incredibly ghetto retreat
        val r = region.subRegionAs<AndroidRegion>(1058, 700, 100, 3) // required for pan right
        logger.info("Zoom in")
        region.pinch(
            Random.nextInt(350, 400),
            Random.nextInt(850, 900),
            0.0,
            100
        )
        delay(500)
        logger.info("Pan right")
        r.swipeTo(r.copy(x = r.x - 800))
        delay(1000)
        // combat echelon should be on the screen at this point
        region.subRegion(1419, 504, 100, 100).click() // this should be where our echelon is at
        delay(1000)
        mapRunnerRegions.retreat.click()
        delay(1000)
        region.subRegion(1115, 696, 250, 95).click()
        delay(1000)
        // end

        // zoom out before terminating for the next mission
        logger.info("Zooming out before terminating")
        region.pinch(
            Random.nextInt(900, 1000),
            Random.nextInt(300, 400),
            0.0,
            1000
        )
        terminateMission()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click()

        logger.info("Selecting node: ${nodes[3]}")
        nodes[3].findRegion().click(); yield()

        logger.info("Selecting node: ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}