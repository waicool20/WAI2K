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
import com.waicool20.wai2k.script.modules.combat.AbsoluteMapRunner
import com.waicool20.wai2k.script.modules.combat.EventMapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlin.random.Random

class EventDJMax_1_1(scriptComponent: ScriptComponent) : AbsoluteMapRunner(scriptComponent),
    EventMapRunner {
    private val logger = loggerFor<EventDJMax_1_1>()

    override suspend fun enterMap() {
        region.subRegion(695, 516, 467, 233).click()
        delay(1000)
        region.subRegion(1454, 842, 325, 110).click()
        delay(4000)
    }

    override suspend fun begin() {
        if (scriptRunner.justRestarted) {
            region.pinch(
                Random.nextInt(700, 800),
                Random.nextInt(300, 400),
                0.0,
                500
            )
            delay(2000)
        }
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])
        mapRunnerRegions.planningMode.click()
        nodes[1].findRegion().click()
        mapRunnerRegions.executePlan.click()
        waitForTurnEnd(4)
        handleBattleResults()
    }
}