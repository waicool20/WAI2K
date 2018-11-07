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

package com.waicool20.wai2k.script.modules

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.DollType
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

class CombatModule(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {
    private val logger = loggerFor<CombatModule>()
    override suspend fun execute() {
        if (!profile.combat.enabled) return
        switchDolls()
    }

    private suspend fun switchDolls() {
        navigator.navigateTo(LocationId.FORMATION)
        logger.info("Switching doll 2 of echelon 1")
        // Doll 2 region ( excludes stuff below name/type )
        region.subRegion(612, 167, 263, 667).clickRandomly()
        delay(100)
        applyFilters(1)
    }

    private suspend fun applyFilters(doll: Int) {
        logger.info("Applying doll filters for dragging doll $doll")
        val stars: Int
        val type: DollType
        with(profile.combat) {
            when (doll) {
                1 -> {
                    stars = doll1Stars
                    type = doll1Type
                }
                2 -> {
                    stars = doll2Stars
                    type = doll2Type
                }
                else -> error("Invalid doll: $doll")
            }
        }

        // Filter By button
        val filterButtonRegion = region.subRegion(1765, 348, 257, 161)
        filterButtonRegion.clickRandomly(); yield()
        // Filter popup region
        region.subRegion(900, 159, 834, 910).run {
            logger.info("Resetting filters")
            find("filters/reset.png").clickRandomly(); delay(300)
            filterButtonRegion.clickRandomly(); yield()
            logger.info("Applying filter $stars star")
            find("filters/${stars}star.png").clickRandomly(); delay(100)
            logger.info("Applying filter $type")
            find("filters/$type.png").clickRandomly(); delay(100)
            logger.info("Confirming filters")
            find("filters/confirm.png").clickRandomly(); delay(100)
        }
    }
}