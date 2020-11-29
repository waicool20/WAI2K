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

class EventSC2_4_EX(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent), EventMapRunner {
    private val logger = loggerFor<EventSC2_4_EX>()
    override val isCorpseDraggingMap = false
    override val ammoResupplyThreshold = 0.2

    override suspend fun enterMap() {
        SCUtils.enterChapter(this)
        SCUtils.setDifficulty(this)

        val r = region.subRegionAs<AndroidRegion>(1250, 325, 250, 150)
        while (isActive) {
            logger.info("Entering map")

            region.matcher.settings.matchDimension = ScriptRunner.HIGH_RES
            val entrance = region.findBest(FileTemplate("$PREFIX/map-entrance.png", 0.80))
            region.matcher.settings.matchDimension = ScriptRunner.NORMAL_RES

            // Last map is saved until restart, enter immediately
            if (entrance != null) {
                entrance.region.click()
                delay((2000 * gameState.delayCoefficient).roundToLong())
                region.subRegion(1834, 590, 229, 109).click() // Confirm start
                break
            }

            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                0.0,
                1000)
            logger.info("Panning right")
            repeat(2) {
                r.swipeTo(r.copy(x = r.x - 1000))
            }
            delay(1000)
        }
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            val r = region.subRegionAs<AndroidRegion>(300, 150, 250, 250)
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                0.0,
                1000)

            logger.info("pan up")
            r.swipeTo(r.copy(y = r.y + 600))
            gameState.requiresMapInit = false
        }
        delay((900 * gameState.delayCoefficient).roundToLong())

        val rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(rEchelons)

        turn1()
        waitForTurnAssets(listOf(FileTemplate("combat/battle/plan.png", 0.96)), false)
        delay(1500)
        mapRunnerRegions.endBattle.click()

        // Map will pan after battle
        waitForGNKSplash(20000); delay(2000)
        mapH = null
        turn2()

        retreatEchelons(Retreat(nodes[0], true))
        terminateMission()
    }

    private suspend fun turn1() {
        logger.info("Enter planning")
        mapRunnerRegions.planningMode.click()

        logger.info("Selecting combat team at command center")
        nodes[0].findRegion().click()

        logger.info("Selecting node: ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting node: ${nodes[1]}")
        nodes[1].findRegion().click()

        mapRunnerRegions.executePlan.click()
    }

    private suspend fun turn2() {
        deployEchelons(nodes[0]); delay(1000)

        logger.info("Select combat team")
        nodes[1].findRegion().click(); delay(1000)

        logger.info("Select dummy")
        nodes[0].findRegion().click(); delay(1000)

        logger.info("Switching")
        region.findBest(FileTemplate("assets/combat/battle/switch.png", 0.90))?.region?.click()
        delay(3000)
    }
}