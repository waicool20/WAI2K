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
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventASNC_4(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventASNC_4>()
    override val rationsResupplyThreshold = 0.2
    override val ammoResupplyThreshold = 0.2

    override suspend fun enterMap() {
        region.subRegion(1334, 704, 468, 234).click() // click ASNC map 4
        delay((900 * gameState.delayCoefficient).roundToLong())
        region.subRegion(1454, 844, 327, 110).click() // enter battle
        region.waitHas(FileTemplate("combat/battle/start.png"), 8000)
    }

    override suspend fun begin() {
        val r = region.subRegionAs<AndroidRegion>(1058, 700, 100, 3) // required for pan up
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            region.pinch(
                Random.nextInt(800, 900),
                Random.nextInt(300, 400),
                0.0,
                800
            )
            delay(500)
            logger.info("Pan up") // top will be obscured need to pan up
            r.swipeTo(r.copy(y = r.y + 200))
            delay(500)
            gameState.requiresMapInit = false
        }
        delay((900 * gameState.delayCoefficient).roundToLong())

        val rEchelons = deployEchelons(nodes[0]) // dynamically resupply echelon 1
        deployEchelons(nodes[1])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()
        resupplyEchelons(rEchelons)
        planPath() // clear right node then go back to heliport
        waitForTurnAndPoints(1, 0, false) // turn does not end so wait for plan to be executed
        mapH = null // map moves after planned path, force rescan of map
        retreatEchelons(Retreat(nodes[0], true))
        terminateMission()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}