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
        region.find("combat/battle/start.png").clickRandomly(); yield()
        resupplyEchelons()
    }

    private suspend fun deployEchelons() {
        logger.info("Deploying echelon 1 to heliport")
        region.clickUntilGone("$PREFIX/heliport.png", 10)
        region.find("ok.png").clickRandomly()

        delay(200)

        logger.info("Deploying echelon 2 to command post")
        region.clickUntilGone("$PREFIX/commandpost.png", 10)
        region.find("echelons/echelon2.png").clickRandomly(); yield()
        region.find("ok.png").clickRandomly(); yield()

        logger.info("Deployment complete")
    }

    private suspend fun resupplyEchelons() {
        region.find("$PREFIX/commandpost-deployed.png").grow(0, 135, 0, 135)
                .clickRandomly()
        delay(200)
        region.clickUntilGone("combat/battle/resupply.png")

        region.findOrNull("close.png")?.clickRandomly()

    }
}