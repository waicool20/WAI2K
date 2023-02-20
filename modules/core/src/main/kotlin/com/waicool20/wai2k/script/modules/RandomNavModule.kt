/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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

import com.waicool20.wai2k.game.location.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.util.formatted
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.random.Random

class RandomNavModule(navigator: Navigator) : ScriptModule(navigator) {

    private var nextTime = randomFutureInstant()

    val logger = loggerFor<RandomNavModule>()

    override suspend fun execute() {
        // Workaround for script coroutine being non-responsive when only running logistics
        // Try to move to a random location then back
        if (Instant.now() < nextTime) return
        if (!profile.logistics.enabled || profile.combat.enabled) return
        nextTime = randomFutureInstant()
        logger.info("Doing random navigation")
        val origin = gameState.currentGameLocation.id

        val dest = listOf(
            LocationId.RESEARCH_MENU,
            LocationId.FACTORY_MENU,
            LocationId.COMBAT_MENU,
            LocationId.FORMATION,
        ).random()

        navigator.navigateTo(dest)
        val dwell = Random.nextLong(60000)
        logger.info("Dwelling for $dwell ms")
        delay(dwell)
        navigator.navigateTo(origin)
        logger.info("Next random nav: ${nextTime.formatted()}")
    }

    private fun randomFutureInstant(): Instant {
        return Instant.now().plusSeconds(Random.nextLong(10, 30) * 60)
    }
}
