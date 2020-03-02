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
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

class EventScarecrowBox(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : EventMapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<EventScarecrowBox>()
    override val isCorpseDraggingMap = false
    override val extractBlueNodes = false

    override suspend fun enterMap() {
        region.subRegion(1240, 357, 63, 41).click()
        delay(500)
        region.subRegion(1762, 880, 231, 111).click()
        delay(4000)
    }

    override suspend fun execute() {
        region.subRegionAs<AndroidRegion>(1100, 362, 10, 10)
                .swipeTo(region.subRegionAs(1100, 362 + 400, 10, 10))
        val rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(rEchelons)

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()

        waitForTurnEnd(3)
        handleBattleResults()
    }
}