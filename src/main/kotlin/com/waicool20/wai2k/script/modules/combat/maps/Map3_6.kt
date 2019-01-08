/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by joo
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

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.combat.MapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

class Map3_6(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map3_6>()
    override val isCorpseDraggingMap = false

    override suspend fun execute() {
        deployEchelons(
                COMMAND_POST to region.subRegion(1734, 329, 103, 113),
                HELIPORT to region.subRegion(1187, 557, 60, 60)
        )
        mapRunnerRegions.startOperation.clickRandomly(); yield()
        waitForGNKSplash()
        resupplyEchelon(HELIPORT, region.subRegion(1187, 557, 60, 60))
        planPath()
        waitForTurnEnd(1)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.clickRandomly(); yield()
        logger.info("Selecting echelon at heliport")
        region.subRegion(1187, 557, 60, 60)
                .clickRandomly(); yield()

        logger.info("Selecting node 1")
        region.subRegion(922, 582, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 2")
        region.subRegion(667, 868, 60, 60)
                .clickRandomly(); yield()

        //Pan down
        region.subRegion(784, 845, 240, 100).let {
            it.swipeToRandomly(it.offset(0, -800 ), 1500); yield()
        }

        logger.info("Selecting node 3")
        region.subRegion(618, 407, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 4")
        region.subRegion(846, 559, 60, 60)
                .clickRandomly(); yield()
        logger.info("Executing plan")
        mapRunnerRegions.executePlan.clickRandomly(); yield()
    }
}