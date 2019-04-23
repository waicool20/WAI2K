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
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

class Map0_2(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map0_2>()
    override val isCorpseDraggingMap = true

    private val heliportDeployment = HELIPORT at region.subRegion(410, 497, 60, 60)
    private val commandPostDeployment = COMMAND_POST at region.subRegion(1054, 487, 110, 110)

    override suspend fun execute() {
        deployEchelons(commandPostDeployment, heliportDeployment)
        mapRunnerRegions.startOperation.clickRandomly(); yield()
        waitForGNKSplash()
        resupplyEchelon(heliportDeployment)
        planPath()
        waitForTurnEnd(3)
        waitForGNKSplash(20)
        planPath2()
        waitForTurnEnd(2)
        handleBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.clickRandomly(); yield()

        logger.info("Selecting echelon at command post")
        region.subRegion(1054, 487, 110, 110)
                .clickRandomly(); yield()
        logger.info("Selecting node 1")
        region.subRegion(811, 350, 60, 60)
                .clickRandomly(); yield()

        // Pan up
        region.subRegion(1020, 110, 240, 10).let {
            it.swipeToRandomly(it.offset(0, 600), 500); yield()
            it.swipeToRandomly(it.offset(0, 600), 500); delay(300)
        }

        logger.info("Selecting node 2")
        region.subRegion(873, 920, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 3")
        region.subRegion(1100, 572, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 4")
        region.subRegion(853, 412, 60, 60)
                .clickRandomly(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.clickRandomly()
    }

    private suspend fun planPath2() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.clickRandomly(); yield()

        logger.info("Selecting echelon at node 4")
        region.subRegion(853, 412, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 5")
        region.subRegion(1337, 409, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 6")
        region.subRegion(1647, 472, 60, 60)
                .clickRandomly(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.clickRandomly()
    }
}