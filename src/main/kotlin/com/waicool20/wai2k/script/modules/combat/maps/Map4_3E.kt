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

class Map4_3E(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map4_3E>()
    override val isCorpseDraggingMap = true

    override suspend fun execute() {
        deployEchelons()
        mapRunnerRegions.startOperation.clickRandomly(); yield()
        resupplyEchelons()
        delay(200)
        planPath()
        waitForBattleEnd()
        handleBattleResults()
    }

    private suspend fun deployEchelons() {
        logger.info("Deploying echelon 1 to heliport")
        logger.info("Pressing the heliport")
        region.subRegion(1788, 723, 60, 60)
                .clickRandomly(); delay(300)
        logger.info("Pressing the ok button")
        mapRunnerRegions.deploy.clickRandomly()
        delay(300)
        logger.info("Deploying echelon 2 to command post")
        logger.info("Pressing the command post")
        region.subRegion(320, 489, 103, 113)
                .clickRandomly(); delay(300)
        logger.info("Pressing the ok button")
        mapRunnerRegions.deploy.clickRandomly()
        delay(300)
        logger.info("Deployment complete")
    }

    private suspend fun resupplyEchelons() {
        logger.info("Waiting for G&K splash screen")
        // Wait for the G&K splash to appear within 10 seconds
        region.waitSuspending("$PREFIX/splash.png", 10).apply {
            logger.info("G&K splash screen appeared")
            delay(1500)
        } ?: logger.info("G&K splash screen did not appear")

        logger.info("Resupplying echelon at command post")
        //Clicking twice, first to highlight the echelon, the second time to enter the deployment menu
        logger.info("Selecting echelon")
        region.subRegion(320, 489, 103, 113).apply {
            clickRandomly(); yield()
            clickRandomly(); delay(300)
        }
        logger.info("Resupplying")
        mapRunnerRegions.resupply.clickRandomly()
        // Close dialog in case echelon doesn't need resupply
        region.findOrNull("close.png")?.clickRandomly(); yield()
        logger.info("Resupply complete")
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.clickRandomly(); yield()
        logger.info("Selecting echelon at heliport")
        region.subRegion(1788, 723, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 1")
        region.subRegion(1766, 464, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 2")
        region.subRegion(1863, 197, 60, 60)
                .clickRandomly(); yield()

        // Pan up
        region.subRegion(1033, 150, 240, 50).randomLocation().let {
            region.swipeRandomly(it, it.offset(0, 850), 800)
            delay(300)
        }

        logger.info("Selecting node 3")
        region.subRegion(1708, 726, 60, 60)
                .clickRandomly(); yield()
        logger.info("Selecting node 4")
        region.subRegion(1731, 368, 60, 60)
                .clickRandomly(); yield()
        logger.info("Executing plan")
        mapRunnerRegions.executePlan.clickRandomly(); yield()
    }
}