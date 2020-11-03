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
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToLong
import kotlin.random.Random

class EventHalloweenSoap(scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent), EventMapRunner {
    private val logger = loggerFor<EventHalloweenSoap>()
    override val isCorpseDraggingMap = false

    override val rationsResupplyThreshold = 0.5

    override suspend fun enterMap() {
        logger.info("Entering map")
        region.subRegion(783, 251, 452, 216).click() // Spoiled Staccato
        delay(2000)
        region.subRegion(1454, 842, 327, 112).click() // Normal Battle
        delay(1000)
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
            delay((1000 * gameState.delayCoefficient).roundToLong())
            gameState.requiresMapInit = false

        }

        deployEchelons(nodes[0], nodes[1]) //teams with only 1 dol in them viable here
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0], nodes[1]) // Cant check supplies on teams with only 1 doll

        planPath()
        waitForTurnEnd(4, false)
        delay(1000)
        handleBattleResults() // ends automatically when enemies killed
    }

    private suspend fun planPath() {
        val r = region.subRegion(900, 110, 200, 300) // probably empty region

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[2]}"); yield()
        nodes[2].findRegion().click()

        r.click(); delay(500) // deselect team 1

        logger.info("Selecting echelon at Command Post")
        nodes[0].findRegion().click()

        logger.info("Selecting ${nodes[3]}"); yield()
        nodes[3].findRegion().click()

        r.click(); delay(500) // deselect team 2

        logger.info("Selecting echelon at heliport")
        nodes[1].findRegion().click()

        logger.info("Selecting ${nodes[4]}"); yield()
        nodes[4].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}