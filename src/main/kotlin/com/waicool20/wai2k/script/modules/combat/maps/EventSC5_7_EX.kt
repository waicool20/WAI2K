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
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventSC5_7_EX(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent), EventMapRunner {
    private val logger = loggerFor<EventSC5_7_EX>()
    override val isCorpseDraggingMap = true
    override val ammoResupplyThreshold = 0.5
    override val rationsResupplyThreshold = 0.5

    override suspend fun enterMap() {
        SCUtils.enterChapter(this)
        SCUtils.setDifficulty(this)

        val r = region.subRegionAs<AndroidRegion>(1700, 250, 250, 150)
        while (isActive) {
            logger.info("Entering map")

            region.matcher.settings.matchDimension = ScriptRunner.HIGH_RES
            val entrance = region.findBest(FileTemplate("$PREFIX/map-entrance.png", 0.90))
            region.matcher.settings.matchDimension = ScriptRunner.NORMAL_RES

            // Last map is saved until restart, enter immediately
            if (entrance != null) {
                entrance.region.click()
                delay((2000 * gameState.delayCoefficient).roundToLong())
                region.subRegion(1834, 590, 229, 109).click() // Confirm start
                break
            }

            logger.info("Panning right")
            r.swipeTo(r.copy(x = r.x - 1000), 600)
            delay(1000)
        }
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(800, 900),
                Random.nextInt(300, 400),
                0.0,
                1000)
            delay(500)
        }
        delay((900 * gameState.delayCoefficient).roundToLong())
        val r = region.subRegion(100, 200, 200, 200)

        val rEchelons = deployEchelons(nodes[0], nodes[1])
        gameState.requiresMapInit = false // For heavy port

        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()

        resupplyEchelons(rEchelons)
        if (rEchelons.isNotEmpty()) {
            r.click(); delay(500) // Adjacent nodes
        }
        resupplyEchelons(nodes[1]); delay(500)

        setAiEchelon()
        planPath()

        waitForTurnAndPoints(2, 3, false)
        delay((900 * gameState.delayCoefficient).roundToLong())
        mapH = null // Scrolls after turn

        r.click(); delay(500) // Deselect combat team
        deployEchelons(nodes[0]); delay(1000)
        swapEchelons(nodes[0], nodes[4])

        retreatEchelons(Retreat(nodes[1], false), Retreat(nodes[0], false))
        delay(500)
        terminateMission()
    }

    private suspend fun setAiEchelon() {
        // Sets M4 so she doesn't run into the zombie king
        logger.info("Setting AI M4 to capture")
        nodes[2].findRegion().click()
        delay(500)
        nodes[2].findRegion().click()
        region.waitHas(FileTemplate("combat/battle/capture-hq-ai.png"), 3000)?.click()
        delay(500)
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); delay(500)

        logger.info("Selecting drag team at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting node ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Selecting node ${nodes[4]}")
        nodes[4].findRegion().click(); yield()

        mapRunnerRegions.executePlan.click()
    }
}