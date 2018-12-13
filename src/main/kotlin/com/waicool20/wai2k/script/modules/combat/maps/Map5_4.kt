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

class Map5_4(
scriptRunner: ScriptRunner,
region: AndroidRegion,
config: Wai2KConfig,
profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map5_4>()
    override val isCorpseDraggingMap = true

    override suspend fun execute() {
        deployEchelons()
        mapRunnerRegions.startOperation.clickRandomly(); yield()
        resupplyEchelons()
        planPath()
        waitForBattleEnd()
        handleBattleResults()
    }

    private suspend fun deployEchelons() {
        logger.info("Deploying echelon 1 to heliport")
        logger.info("Pressing the heliport")
        region.subRegion(295, 320, 87, 83)
                .clickRandomly(); yield()
        logger.info("Pressing the ok button")
        mapRunnerRegions.deploy
                .clickRandomly()
        delay(200)
        logger.info("Deploying echelon 2 to command post")
        logger.info("Pressing the command post")
        region.subRegion(1715, 233, 103, 113)
                .clickRandomly(); yield()
        logger.info("Pressing the ok button")
        mapRunnerRegions.deploy
                .clickRandomly()
        delay(200)
        logger.info("Deployment complete")
    }

    private suspend fun resupplyEchelons() {
        logger.info("Finding the G&K splash")
        // Wait for the G&K splash to appear within 10 seconds
        region.waitSuspending("$PREFIX/splash.png", 10).apply {
            logger.info("Found the splash!")
        } ?: logger.info("Cant find the splash!")

        delay(2000)

        logger.info("Resupplying echelon at command post")
        //Clicking twice, first to highlight the echelon, the second time to enter the deployment menu
        logger.info("Selecting echelon")
        region.subRegion(1715, 233, 103, 113).apply {
                clickRandomly(); yield()
                clickRandomly(); yield()
        }
        logger.info("Found the resupply button")
        logger.info("Pressing the resupply button")
        mapRunnerRegions.resupply
                .clickRandomly()
        // Close dialog in case echelon doesn't need resupply
        region.findOrNull("close.png")
                ?.clickRandomly()
        delay(200)
        logger.info("Resupply complete")
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode
                .clickRandomly(); yield()
        logger.info("Selecting echelon at heliport")
        region.subRegion(295, 320, 87, 83)
                .clickRandomly()
        logger.info("Selecting node 1")
        region.subRegion(536, 260, 82  , 46)
                .clickRandomly(); yield()
        logger.info("Selecting node 2")
        region.subRegion(788, 242, 66, 56)
                .clickRandomly(); yield()
        logger.info("Selecting node 3")
        region.subRegion(1056, 248, 92, 75)
                .clickRandomly(); yield()
        logger.info("Selecting node 4")
        region.subRegion(1088, 476, 61, 55)
                .clickRandomly(); yield()
        logger.info("Selecting node 5")
        region.subRegion(1055, 641, 102, 102)
                .clickRandomly(); yield()
        logger.info("Executing plan")
        mapRunnerRegions.executePlan
                .clickRandomly(); yield()
    }
}
