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
import com.waicool20.waicoolutils.binarizeImage
import com.waicool20.waicoolutils.countColor
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.awt.Color
import kotlin.math.roundToLong
import kotlin.random.Random

class Map10_4E_Drag(scriptComponent: ScriptComponent) : AbsoluteMapRunner(scriptComponent) {
    private val logger = loggerFor<Map10_4E_Drag>()
    override val isCorpseDraggingMap = true

    override suspend fun begin() {

        // Mostly empty region to the left
        val r = region.subRegionAs<AndroidRegion>(300, 500, 150, 8)
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(700, 800),
                Random.nextInt(200, 300),
                0.0,
                500
            ) // It's pretty close to the post init zoom
            delay((1000 * gameState.delayCoefficient).roundToLong())
            r.swipeTo(r.copy(y = r.y + 14)) // Nudge it anyway
        }

        delay((800 * gameState.delayCoefficient).roundToLong()) // Map sometimes lags when starting
        val rEchelons = deployEchelons(nodes[0])
        openEchelon(nodes[1], singleClick = true); delay(300)
        checkDragRepairs()

        logger.info("Panning down")
        r.swipeTo(r.copy(y = r.y - 200)) // Random, the nodes should still line up

        delay(500)
        deployEchelons(nodes[2])

        logger.info("Panning up")
        r.swipeTo(r.copy(y = r.y + 200))
        delay(500)

        gameState.requiresMapInit = false // Yes this gets set every time

        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(rEchelons + nodes[1])
        retreatEchelons(nodes[1]); delay(300)

        planPath()
        waitForTurnEnd(5, false); delay(1000)
        waitForTurnAssets(false, 0.96, "combat/battle/plan.png")
        retreatEchelons(nodes[5])
        terminateMission() // Make a restart option for speed ;)
    }

    private suspend fun planPath() {

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Selecting ${nodes[4]}")
        nodes[4].findRegion().click(); yield()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }

    private suspend fun checkDragRepairs() {

        // Checks if the other doll has less than 5 links from possible grenade chip damage
        // Taken from MapRunner deployEchelons
        val hpImage = region.subRegion(373, 778, 217, 1).capture().binarizeImage()
        val hp = hpImage.countColor(Color.WHITE) / hpImage.width.toDouble() * 100
        if (hp <= 80) {
            logger.info("Repairing other combat doll that has lost a dummy link")
            region.subRegion(360, 286, 246, 323).click()
            region.subRegion(1360, 702, 290, 117)
                .waitHas(FileTemplate("ok.png"), 3000)?.click()
            scriptStats.repairs++
            delay(1500)
        }
        mapRunnerRegions.deploy.click(); delay(500)
    }
}