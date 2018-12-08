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
        region.subRegion(1745, 914, 240, 100).clickRandomly(); yield()
        resupplyEchelons()
        planPath()
        waitForBattleEnd()
        handleBattleResults()
    }

    private suspend fun deployEchelons() {
        logger.info("Deploying echelon 1 to heliport")
        region.subRegion(295, 320, 87, 83)
                .clickRandomly(); yield()
        region.subRegion(1770, 930, 180, 60)
                .clickRandomly(); yield()
        logger.info("Deploying echelon 2 to command post")
        region.subRegion(1700, 213, 136, 139)
                .clickRandomly(); yield()
        region.subRegion(1770, 930, 180, 60)
                .clickRandomly(); yield()
        logger.info("Deployment complete")
    }

    private suspend fun resupplyEchelons() {
        logger.info("Finding the G&K splash")

        region.waitSuspending("$PREFIX/splash.png", 10).apply {
            logger.info("Found the splash!")
        } ?: logger.info("Cant find the splash!")

        delay(2000)

        logger.info("Resupplying echelon at command post")
        logger.info("Selecting echelon")
        region.subRegion(1700, 213, 136, 139).apply {
                clickRandomly(); yield()
                clickRandomly(); yield()
        }
        logger.info("Found the resupply button")
        logger.info("Resupplying........")
        region.subRegion(1753, 804, 260, 70)
                .clickRandomly();yield()
        logger.info("Checking for errors......")
        region.findOrNull("close.png")
                ?.clickRandomly(); yield()
        logger.info("Resupply complete")
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        region.subRegion(5, 860, 200, 50)
                .clickRandomly(); yield()
        logger.info("Selecting echelon at heliport")
        region.subRegion(295, 320, 87, 83)
                .clickRandomly(); yield()
        logger.info("Selecting node 1")
        region.subRegion(530, 223, 92, 89)
                .clickRandomly(); yield()
        logger.info("Selecting node 2")
        region.subRegion(774, 222, 92, 89)
                .clickRandomly(); yield()
        logger.info("Selecting node 3")
        region.subRegion(1040, 225, 124, 119)
                .clickRandomly(); yield()
        logger.info("Selecting node 4")
        region.subRegion(1069, 456, 97, 93)
                .clickRandomly(); yield()
        logger.info("Selecting node 5")
        region.subRegion(1038, 618, 132, 136)
                .clickRandomly(); yield()

        logger.info("Executing plan")
        region.subRegion(1895, 922, 227, 128)
                .clickRandomly(); yield()
    }
}
