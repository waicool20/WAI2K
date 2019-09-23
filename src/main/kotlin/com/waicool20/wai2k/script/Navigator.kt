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
import com.waicool20.wai2k.game.GFL
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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

    companion object {
        private val transitionDelays = LinkedList<Long>()
        private var restartCounter = 0
    }

    /**
     * Finds the current location
     *
     * @return Current [GameLocation]
     */
    suspend fun identifyCurrentLocation(retries: Int = 3): GameLocation {
        logger.info("Identifying current location")
        val locations = locations.entries.sortedBy { it.value.isIntermediate }
                .map { it.value }.asFlow()
        repeat(retries) { i ->
            checkLogistics(true)
            try {
                return locations.flatMapMerge { loc ->
                    flow { emit(loc.takeIf { loc.isInRegion(region) }) }
                }.filterNotNull().first()
            } catch (e: NoSuchElementException) {
                logger.warn("Could not find location after ${i + 1} attempts, retries remaining: ${retries - i - 1}")
                delay(1000)
            }
        }
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
                // Flag for ignoring transition delay because of logistics
                var skipTransitionDelay = false
                var ticks = 0
                // Record starting transition time
                val startTransitionTime = System.currentTimeMillis()
                // Record amount of time that is spent on none transitional delay like image matching
                var noneTransitionDelay = 0L
                val avgTransitionDelay = transitionDelays.takeIf { it.isNotEmpty() }
                        ?.average()?.roundToLong() ?: config.gameRestartConfig.averageDelay
                for (cycle in 0..Integer.MAX_VALUE) {
                    if (cycle % 5 == 0) {
                        link.asset.getSubRegionFor(region).let {
                            // Shrink region slightly to 90% of defined size
                            it.grow((it.w * -0.1).roundToInt(), (it.h * -0.1).roundToInt())
                        }.clickRandomly()
                        // Wait around average transition delay if not an intermediate location
                        // since it cant transition immediately
                        if (!srcLoc.isIntermediate) {
                            delay(avgTransitionDelay + config.scriptConfig.baseNavigationDelay)
                            ticks++
                        }
                    }
                    val ntdStart = System.currentTimeMillis()
                    if (!srcLoc.isInRegion(region)) break
                    // Source will always be on screen if it is an intermediate menu
                    if (srcLoc.isIntermediate && destLoc.isInRegion(region)) {
                        skipDestinationCheck = true
                        break
                    }
                    noneTransitionDelay += System.currentTimeMillis() - ntdStart
                    if (checkLogistics()) skipTransitionDelay = true
                }

                logger.info("Waiting for transition to ${dest.id}")
                if (!skipDestinationCheck) {
                    val ntdStart = System.currentTimeMillis()
                    // Re navigate if destination doesnt come up after timeout, make this a setting?
                    val timeout = 20
                    val atDestination = withTimeoutOrNull(timeout * 1000L) {
                        while (isActive) {
                            if (destLoc.isInRegion(region)) return@withTimeoutOrNull true
                            if (checkLogistics()) skipTransitionDelay = true
                        }
                        false
                    }

                    if (atDestination != true) {
                        logger.info("Destination not on screen after ${timeout}s, will try to re-navigate")
                        continue@retry
                    }
                    noneTransitionDelay += System.currentTimeMillis() - ntdStart
                }

                // Calculate the transition delays and delay coefficients
                // Update the restart counter as needed
                val transitionTime = System.currentTimeMillis() - startTransitionTime
                if (!skipTransitionDelay) {
                    transitionDelays.add(transitionTime - avgTransitionDelay * ticks - noneTransitionDelay)
                    if (transitionDelays.size >= 20) transitionDelays.removeFirst()
                }
                gameState.delayCoefficient = avgTransitionDelay.toDouble() / config.gameRestartConfig.averageDelay
                if (gameState.delayCoefficient > config.gameRestartConfig.delayCoefficientThreshold) {
                    restartCounter++
                } else {
                    if (restartCounter > 0) restartCounter--
                }

                updateAverageDelay(avgTransitionDelay)
                updateRestartFlag()
                logger.info("Transition: $transitionTime ms" +
                        " | Delay: $avgTransitionDelay ms" +
                        " | Ticks: $ticks" +
                        " | DC: ${DecimalFormat("#.##").format(gameState.delayCoefficient)}" +
                        " | RC: $restartCounter"
                )

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
     * Automatically adjust the current average delay if we got some significant change
     * We pass it through a smoothing function so it doesn't change too drastically
     * in case the new value is actually just an edge case. This function heavily favors
     * decreasing over increasing the value so that the value doesn't rise quickly enough to
     * stop a restart
     */
    private fun updateAverageDelay(avgTransitionDelay: Long) {
        val oldDelay = config.gameRestartConfig.averageDelay
        val newDelay = when {
            gameState.delayCoefficient <= 0.9 -> oldDelay + (avgTransitionDelay - oldDelay) / 5.0
            gameState.delayCoefficient >= config.gameRestartConfig.delayCoefficientThreshold -> {
                oldDelay + (avgTransitionDelay - oldDelay) / 500.0
            }
            else -> return
        }.roundToLong()
        logger.info("Auto adjusting average delay from $oldDelay to $newDelay")
        config.gameRestartConfig.averageDelay = newDelay
        config.save()
    }

    /**
     * Updates the gamestate restart flag if the restart counter exceeds the threshold (10)
     */
    private fun updateRestartFlag() {
        if (config.gameRestartConfig.enabled && !gameState.requiresRestart && restartCounter >= 10) {
            logger.info("Game needs to restart since the delays are getting too long")
            gameState.requiresRestart = true
        }
    }

    private val logisticWaitList = listOf(LocationId.HOME, LocationId.HOME_STATUS, LocationId.UNKNOWN)

    /**
     * Checks if there are logistics, if there were then try and receive them
     *
     * @return true if any logistics arrived
     */
    suspend fun checkLogistics(forceCheck: Boolean = false): Boolean {
        // If gamestate is up to date then we can rely on timers or not
        // to see if logistics might arrive anytime soon
        // We skip further execution if no logistics is due in 15s
        if (!forceCheck && !gameState.requiresUpdate &&
                gameState.echelons.mapNotNull { it.logisticsSupportAssignment }
                        .none { Duration.between(Instant.now(), it.eta).seconds <= 15 }) return false
        var logisticsArrived = false
        while (true) {
            if (region.has("navigator/logistics_arrived.png")) {
                logger.info("An echelon has arrived from logistics")
                region.clickRandomly()
                delay(500)
            }

            // Even if the logistics arrived didnt show up, its possible
            // that it was clicked through by some other function
            if (region.doesntHave("navigator/logistics_dialog.png")) break

            logisticsArrived = true

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
                "ok_large.png"
            } else "cancel.png"

            region.waitSuspending(image, 10)?.clickRandomly()
            scriptStats.logisticsSupportReceived++

            // Mark game state dirty, needs updating
            gameState.requiresUpdate = true
            if (logisticWaitList.any { gameState.currentGameLocation.id == it }) {
                // Wait a bit in case another echelon arrives
                logger.info("Waiting a bit to see if anymore echelons arrive")
                delay(5000)
            }
        }
        return logisticsArrived
    }

    /**
     * Checks for the gamestate restart flag and restarts the game if required
     * This assumes that automatic login is enabled and no updates are required
     */
    suspend fun checkRequiresRestart() {
        if (config.gameRestartConfig.enabled && gameState.requiresRestart) {
            gameState.requiresRestart = false
            restartCounter = 0
            transitionDelays.clear()
            scriptStats.gameRestarts++
            logger.info("Game will now restart")
            region.androidScreen.device.processManager.apply {
                kill(GFL.pkgName)
                delay(200)
                start(GFL.pkgName, GFL.mainActivity)
            }
            logger.info("Game restarted, waiting for login screen")
            region.waitSuspending("login.png", 3600)
            logger.info("Logging in")
            region.subRegion(630, 400, 900, 300).clickRandomly()
            while (locations[LocationId.HOME]?.isInRegion(region) == false) delay(100)
            gameState.currentGameLocation = locations[LocationId.HOME]!!
            logger.info("Logged in")
        }
    }
}