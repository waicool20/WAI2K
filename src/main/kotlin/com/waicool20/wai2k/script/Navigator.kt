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

package com.waicool20.wai2k.script

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.coroutineContext

class Navigator(
        private val gameState: GameState,
        private val region: AndroidRegion,
        private val config: Wai2KConfig,
        private val profile: Wai2KProfile
) {
    private val logger = loggerFor<Navigator>()
    private val locations by lazy { GameLocation.listAll(config, true) }
    /**
     * Finds the current location
     *
     * @return Current [GameLocation]
     */
    suspend fun identifyCurrentLocation(): GameLocation {
        logger.info("Identifying current location")
        val channel = Channel<GameLocation?>()
        val jobs = locations.map {
            launch { channel.send(it.takeIf { it.isInRegion(region) }) }
        }
        channel.consumeEach {
            it?.let { loc ->
                logger.info("GameLocation found: ${loc.name}")
                gameState.currentGameLocation = loc
                return loc
            }
            if (jobs.all { it.isCompleted }) channel.close()
        }
        logger.info("Current location could not be identified")
        coroutineContext.cancelAndYield()
    }

    /**
     * Attempts to navigate to the destination
     *
     * @param destination Name of destination
     */
    suspend fun navigateTo(destination: String) {
        val dest = locations.find { it.name == destination }
                ?: error("Invalid destination: $destination")
        logger.info("Navigating to ${dest.name}")
        if (!gameState.currentGameLocation.isInRegion(region)) identifyCurrentLocation()
        val cLocation = gameState.currentGameLocation
        val path = cLocation.shortestPathTo(dest)
        logger.debug("Found solution: ${path.joinToString("->") { it.first.name }}")
        for ((loc, link) in path) {
            if (gameState.currentGameLocation.isIntermediate && loc.isInRegion(region)) {
                continue
            }
            gameState.currentGameLocation = loc
            logger.info("Going to ${loc.name}")
            link.asset.getSubRegionFor(region).clickRandomly()
            if (!loc.isInRegion(region)) delay(500)
            logger.info("At ${loc.name}")
        }
    }
}