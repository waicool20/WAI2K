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
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

class Navigator(
        private val scriptRunner: ScriptRunner,
        private val region: AndroidRegion,
        private val config: Wai2KConfig,
        private val profile: Wai2KProfile
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = scriptRunner.coroutineContext
    private val logger = loggerFor<Navigator>()
    private val gameState get() = scriptRunner.gameState
    private val scriptStats get() = scriptRunner.scriptStats
    private val locations by lazy { GameLocation.mappings(config, true) }
    /**
     * Finds the current location
     *
     * @return Current [GameLocation]
     */
    suspend fun identifyCurrentLocation(retries: Int = 3): GameLocation {
        logger.info("Identifying current location")
        val channel = Channel<GameLocation?>()
        repeat(retries) { i ->
            checkLogistics()
            val jobs = locations.entries.sortedBy { it.value.isIntermediate }
                    .map { (_, model) ->
                        launch { channel.send(model.takeIf { model.isInRegion(region) }) }
                    }
            for (loc in channel) {
                loc?.let { model ->
                    logger.info("GameLocation found: $model")
                    gameState.currentGameLocation = model
                    return model
                }
                if (jobs.all { it.isCompleted }) break
            }
            logger.warn("Could not find location after ${i + 1} attempts, retries remaining: ${retries - i - 1}")
            delay(1000)
        }
        channel.close()
        logger.warn("Current location could not be identified")
        coroutineContext.cancelAndYield()
    }

    /**
     * Attempts to navigate to the destination
     *
     * @param destination Name of destination
     */
    suspend fun navigateTo(destination: LocationId, retries: Int = 3) {
        retry@ for (r in 0 until retries) {
            val dest = locations[destination] ?: error("Invalid destination: $destination")
            logger.info("Navigating to ${dest.id}")
            val cLocation = gameState.currentGameLocation.takeIf { it.isInRegion(region) }
                    ?: identifyCurrentLocation()
            val path = cLocation.shortestPathTo(dest)
            if (path == null) {
                logger.warn("No known solution from $cLocation to $dest")
                coroutineContext.cancelAndYield()
            }
            if (path.isEmpty()) {
                logger.info("Already at ${dest.id}")
                return
            }
            logger.debug("Found solution: CURRENT->${path.joinToString("->") { "${it.dest.id}" }}")
            for ((srcLoc, destLoc, link) in path) {
                if (gameState.currentGameLocation.isIntermediate && destLoc.isInRegion(region)) {
                    logger.info("At ${destLoc.id}")
                    continue
                }
                logger.info("Going to ${destLoc.id}")
                // Flag for skipping the final destination check or not
                var skipDestinationCheck = false
                // Click the link every 5 ticks, check the region every tick in case the first clicks didn't get it
                var i = 0
                while (isActive) {
                    if (i++ % 5 == 0) {
                        link.asset.getSubRegionFor(region).let {
                            // Shrink region slightly to 90% of defined size
                            it.grow((it.w * -0.1).roundToInt(), (it.h * -0.1).roundToInt())
                        }.clickRandomly()
                        // Wait around 1.5s if not an intermediate location since it cant
                        // transition immediately
                        if (!srcLoc.isIntermediate) delay(1500)
                    }
                    if (!srcLoc.isInRegion(region)) break
                    // Source will always be on screen if it is an intermediate menu
                    if (srcLoc.isIntermediate && destLoc.isInRegion(region)) {
                        skipDestinationCheck = true
                        break
                    }
                    checkLogistics()
                }

                logger.info("Waiting for transition to ${dest.id}")
                if (!skipDestinationCheck) {
                    // Re navigate if destination doesnt come up after timeout, make this a setting?
                    val timeout = 20
                    val atDestination = withTimeoutOrNull(timeout * 1000L) {
                        while (isActive) {
                            if (destLoc.isInRegion(region)) return@withTimeoutOrNull true
                            checkLogistics()
                        }
                        false
                    }

                    if (atDestination != true) {
                        logger.info("Destination not on screen after ${timeout}s, will try to re-navigate")
                        continue@retry
                    }
                }

                gameState.currentGameLocation = destLoc
                checkLogistics()
                if (destLoc.id == destination) {
                    logger.info("At destination $destination")
                    return
                } else {
                    logger.info("At ${destLoc.id}")
                }
            }
        }
    }

    /**
     * Checks if there are logistics, if there were then try and receive them
     */
    suspend fun checkLogistics() {
        // If gamestate is up to date then we can rely on timers or not
        // to see if logistics might arrive anytime soon
        // We skip further execution if no logistics is due in 15s
        if (!gameState.requiresUpdate &&
                gameState.echelons.mapNotNull { it.logisticsSupportAssignment }
                        .none { Duration.between(Instant.now(), it.eta).seconds <= 15 }) return
        while (true) {
            if (region.has("navigator/logistics_arrived.png")) {
                logger.info("An echelon has arrived from logistics")
                region.clickRandomly()
                delay(500)
            }

            // Even if the logistics arrived didnt show up, its possible
            // that it was clicked through by some other function
            if (region.doesntHave("navigator/logistics_dialog.png")) break

            // Continue based on receival mode
            val cont = when (profile.logistics.receiveMode) {
                Wai2KProfile.Logistics.ReceivalMode.ALWAYS_CONTINUE -> {
                    logger.info("Continuing this logistics support")
                    true
                }
                Wai2KProfile.Logistics.ReceivalMode.RANDOM -> {
                    if (Random().nextBoolean()) {
                        logger.info("Randomized receive, continue logistics support this time")
                        true
                    } else {
                        logger.info("Randomized receive, stopping logistics support this time")
                        false
                    }
                }
                Wai2KProfile.Logistics.ReceivalMode.ALWAYS_CANCEL -> {
                    logger.info("Stopping this logistics support")
                    false
                }
                else -> error("Got an invalid ReceivalMode for some reason")
            }
            val image = if (cont) {
                // Increment sent stats if we are continuing
                scriptStats.logisticsSupportSent++
                "confirm.png"
            } else "cancel.png"

            region.waitSuspending(image, 10)?.clickRandomly()
            scriptStats.logisticsSupportReceived++

            // Mark game state dirty, needs updating
            gameState.requiresUpdate = true
            if (gameState.currentGameLocation.id == LocationId.HOME ||
                    gameState.currentGameLocation.id == LocationId.HOME_STATUS ||
                    gameState.currentGameLocation.id == LocationId.UNKNOWN) {
                // Wait a bit in case another echelon arrives
                logger.info("Waiting a bit to see if anymore echelons arrive")
                delay(5000)
            }
        }
    }
}