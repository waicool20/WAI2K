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
import com.waicool20.wai2k.script.modules.combat.CorpseDragging
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventSC2_1(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent), EventMapRunner {
    private val logger = loggerFor<EventSC2_1>()

    // To deselect current team
    private val emptyNode = region.subRegionAs<AndroidRegion>(230, 510, 100, 100)

    override suspend fun enterMap() {
        SCUtils.enterChapter(this)
        SCUtils.setDifficulty(this)

        val r = region.subRegionAs<AndroidRegion>(550, 240, 250, 150)
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

            logger.info("Panning left")
            r.swipeTo(r.copy(x = r.x + 1000))
            delay(1000)
        }
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)
    }

    override suspend fun begin() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(900, 1000),
                Random.nextInt(300, 400),
                0.0,
                1000)

            delay(500)
            logger.info("Pan up")
            emptyNode.swipeTo(emptyNode.copy(y = emptyNode.y + 150))
            gameState.requiresMapInit = false
        }
        delay((900 * gameState.delayCoefficient).roundToLong())

        var rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(rEchelons)

        turn1()
        waitForTurnAssets(listOf(FileTemplate("combat/battle/plan.png", 0.96)), false)

        rEchelons = deployEchelons(nodes[0])
        resupplyEchelons(rEchelons)

        turn2()
        waitForTurnAndPoints(3, 0, false, 240_000) // Auto ends on node capture
        handleBattleResults()
    }

    private suspend fun turn1() {
        logger.info("Moving combat team up")
        emptyNode.click(); delay(500)

        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click(); delay(500)

        logger.info("Selecting node at ${nodes[1]}")
        nodes[1].findRegion().click()
        delay((2500 * gameState.delayCoefficient).roundToLong())

        emptyNode.click(); delay(800)
    }

    private suspend fun turn2() {
        emptyNode.click(); delay(500)

        mapRunnerRegions.planningMode.click(); delay(500)

        logger.info("Selecting team 1 at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Selecting node at ${nodes[2]}")
        nodes[2].findRegion().click()

        emptyNode.click(); delay(500)
        logger.info("Selecting team 2 at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting node at ${nodes[3]}")
        nodes[3].findRegion().click()

        emptyNode.click(); delay(500)
        logger.info("Selecting team 1 at ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Selecting node at ${nodes[4]}")
        nodes[4].findRegion().click()

        emptyNode.click(); delay(500)
        logger.info("Selecting team 2 at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting node at ${nodes[5]}")
        nodes[5].findRegion().click()

        logger.info("Selecting node at ${nodes[6]}")
        nodes[6].findRegion().click()

        mapRunnerRegions.executePlan.click()
    }
}