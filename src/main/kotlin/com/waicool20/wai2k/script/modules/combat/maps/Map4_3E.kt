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

private const val PREFIX = "combat/maps/4-3E"

class Map4_3E(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map4_3E>()

    override suspend fun execute() {
        deployEchelons()
        region.find("combat/battle/start.png").clickRandomly()
        delay(3000)
        resupplyEchelons()
        planPath()
    }

    private suspend fun deployEchelons() {
        logger.info("Deploying echelon 1 to heliport")
        region.clickUntilGone("$PREFIX/heliport.png", 10)
        region.find("ok.png").clickRandomly()

        delay(200)

        logger.info("Deploying echelon 2 to command post")
        region.clickUntilGone("$PREFIX/commandpost.png", 10)
        region.find("echelons/echelon2.png").clickRandomly(); yield()
        region.find("ok.png").clickRandomly()

        delay(200)

        logger.info("Deployment complete")
    }

    private suspend fun resupplyEchelons() {
        logger.info("Resupplying echelon at command post")
        region.find("$PREFIX/commandpost-deployed.png").grow(0, 135, 0, 135).apply {
            clickRandomly(); yield()
            clickRandomly(); yield()
        }

        delay(200)
        region.clickUntilGone("combat/battle/resupply.png")

        region.findOrNull("close.png")?.clickRandomly()
        logger.info("Resupply complete")
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        region.clickUntilGone("combat/battle/plan.png")
        logger.info("Selecting echelon at heliport")
        region.find("$PREFIX/heliport-deployed.png").grow(100, 0, 100, 0)
                .clickRandomly(); yield()
        logger.info("Selecting node 1")
        region.find("$PREFIX/node1.png").grow(90, 0, 0, 0)
                .clickRandomly(); yield()
        logger.info("Selecting node 2")
        region.find("$PREFIX/node2.png").grow(0, 75, 0, 0)
                .clickRandomly(); yield()

        // Pan up
        region.subRegion(1033, 225, 240, 100).let {
            it.swipeToRandomly(it.offset(0, 700), 1500); yield()
        }

        logger.info("Selecting node 3")
        region.find("$PREFIX/node3.png").grow(0, 80, 0, 0)
                .clickRandomly(); yield()

        logger.info("Selecting node 4")
        region.find("$PREFIX/node4.png").grow(0, 110, 0, 0)
                .clickRandomly(); yield()

        logger.info("Executing plan")
        region.clickUntilGone("combat/battle/plan-execute.png")
    }
}