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
import kotlinx.coroutines.yield

class Map5_4(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map5_4>()
    override val isCorpseDraggingMap = true

    override suspend fun execute() {
        deployEchelons(
                HELIPORT to region.subRegion(295, 320, 87, 83),
                COMMAND_POST to region.subRegion(1715, 233, 103, 113)
        )
        mapRunnerRegions.startOperation.clickRandomly(); yield()
        waitForGNKSplash()
        resupplyEchelon(COMMAND_POST, region.subRegion(1715, 233, 103, 113))
        planPath()
        waitForTurnEnd(5)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Selecting echelon at heliport")
        region.subRegion(312, 334, 60, 60)
                .clickRandomly(); yield()

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.clickRandomly(); yield()

        logger.info("Selecting node 1")
        region.subRegion(1073, 254, 60, 60)
                .clickRandomly(); yield()

        logger.info("Selecting node 2")
        region.subRegion(1072, 658, 60, 60)
                .clickRandomly(); yield()
        logger.info("Executing plan")
        mapRunnerRegions.executePlan.clickRandomly(); yield()
    }
}
