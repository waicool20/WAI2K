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

class Map4_6(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map4_6>()
    override val isCorpseDraggingMap = false

    override suspend fun execute() {
        deployment()
        mapRunnerRegions.startOperation.clickRandomly(); yield()
        waitForGNKSplash()
        resupplyEchelon(HELIPORT, region.subRegion(1829, 236, 60, 60))
        planPath()
        waitForTurnEnd(4)
        handleBattleResults()
    }

    private suspend fun deployment() {
        logger.info("Deploying echelon 1 to command post")
        region.subRegion(1721, 605, 103, 113)
                .clickRandomly(); yield()
        mapRunnerRegions.deploy.clickRandomly()
        delay(300)

        //Pan up
        region.subRegion(1300, 100, 240, 100).randomLocation().let { l ->
            repeat(2) { region.swipeRandomly(l, l.offset(0, 700), 400) }
        }

        logger.info("Deploying echelon 2 to heliport")
        region.subRegion(1829, 236, 60, 60)
                .clickRandomly(); yield()
        mapRunnerRegions.deploy.clickRandomly()
        delay(300)

        logger.info("Deployment complete")
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.clickRandomly(); yield()
        logger.info("Selecting echelon at heliport")
        region.subRegion(1829, 236, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 1")
        region.subRegion(1424, 239, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 2")
        region.subRegion(1133, 243, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 3")
        region.subRegion(762, 270, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 4")
        region.subRegion(326, 261, 60, 60)
                .clickRandomly(); yield()
        logger.info("Executing plan")
        mapRunnerRegions.executePlan.clickRandomly(); yield()
    }
}