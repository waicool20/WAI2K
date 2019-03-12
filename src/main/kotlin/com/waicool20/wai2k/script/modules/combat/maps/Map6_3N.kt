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

class Map6_3N(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map6_3N>()
    override val isCorpseDraggingMap = true

    override suspend fun execute() {
        deployEchelons(
                COMMAND_POST to region.subRegion(617, 375, 103, 113)
        )
        mapRunnerRegions.startOperation.clickRandomly(); yield()
        waitForGNKSplash()
        planPath()
        waitForTurnEnd(2)
        waitForGNKSplash(30)
        deployEchelons(
                HELIPORT to region.subRegion(1277, 578, 60, 60)
        )
        delay(200)
        resupplyEchelon(HELIPORT, region.subRegion(1277, 578, 60, 60))
        delay(200)
        retreatEchelon(HELIPORT, region.subRegion(1277, 578, 60, 60))
        delay(1500)
        deploy2ndEchelon()
        delay(800)
        switchDolls()
        retreatEchelon(HELIPORT, region.subRegion(1277, 578, 60, 60))
        delay(1000)
        terminateBattle()
        handleNightBattleResults()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.clickRandomly(); yield()

        logger.info("Selecting echelon at command post")
        region.subRegion(617, 375, 110, 110)
                .clickRandomly(); yield()
        logger.info("Selecting node 1")
        region.subRegion(1078, 437, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 2")
        region.subRegion(1424, 419, 60, 60)
                .clickRandomly(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.clickRandomly()
    }

    private suspend fun deploy2ndEchelon() {
        logger.info("Double Clicking on the Heliport")
        region.subRegion(1277, 578, 60, 60)
                .clickRandomly(); yield()
        region.subRegion(1277, 578, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting the echelon underneath")
        region.subRegion(127, 406, 155, 85)
                .clickRandomly(); yield()
        mapRunnerRegions.deploy.clickRandomly()
        logger.info("Deployment Successful")
    }

    private suspend fun switchDolls() {
        logger.info("Switching echelons for retreat")
        region.subRegion(1424, 419, 60, 60)
                .clickRandomly(); yield()
        region.subRegion(1277, 578, 60, 60)
                .clickRandomly();
        delay(200)
        region.subRegion(1061, 579, 200, 55)
                .clickRandomly(); yield()
        delay(1500)
    }
}