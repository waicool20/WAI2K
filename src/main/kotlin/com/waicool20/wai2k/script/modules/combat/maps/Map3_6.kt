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

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.combat.MapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.yield

class Map3_6(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map3_6>()
    override val isCorpseDraggingMap = false

    private val heliportDeployment = HELIPORT at region.subRegion(1179, 558, 60, 60)
    private val commandPostDeployment = COMMAND_POST at region.subRegion(1740, 357, 60, 60)

    override suspend fun execute() {
        deployEchelons(heliportDeployment, commandPostDeployment)
        mapRunnerRegions.startOperation.clickRandomly(); yield()
        waitForGNKSplash()
        resupplyEchelons(heliportDeployment)
        planPath()
        waitForTurnAssets("$PREFIX/turn-end.png")
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.clickRandomly(); yield()

        //Pan down
        region.subRegion(784, 950, 240, 100).let {
            it.swipeToRandomly(it.offset(0, -900), 1000); yield()
        }

        logger.info("Selecting node 1")
        region.subRegion(848, 557, 60, 60)
                .clickRandomly(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.clickRandomly(); yield()
    }
}