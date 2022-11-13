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
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.wai2k.script.modules.combat.HomographyMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToInt
import kotlin.random.Random

class EventPR_Ranking (scriptComponent: ScriptComponent) : HomographyMapRunner(scriptComponent),
    EventMapRunner {

    private val logger = loggerFor<EventPR_Ranking>()

    override suspend fun enterMap() {
        if (gameState.requiresMapInit) {
            PRUtils.enterInfinity(this, PRUtils.Difficulty.NORMAL)
        }

        // Click map entry
        region.subRegion(1673, 693, 221, 61).click()

        delay(500)

        // Confirm start
        region.subRegion(1832, 589, 232, 111).click()
    }

    override suspend fun begin() {
        logger.info("Zoom out")
        region.pinch(
            Random.nextInt(500, 600),
            Random.nextInt(300, 400),
            0.0,
            500
        )
        logger.info("Zoom in")
        region.pinch(
            Random.nextInt(300, 350),
            Random.nextInt(400, 450),
            0.0,
            500
        )
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        delay(2000)
        // Dismiss mission objective
        region.subRegion(1903, 27, 103, 34).click()
        delay(2000)
        region.subRegion(35, 144, 375, 77).click()
        delay(2000)
        combatSettlement(false)
        handleBattleResults()
    }
}