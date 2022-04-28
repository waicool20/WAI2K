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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.android.ProcessManager
import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.events.GameRestartEvent
import com.waicool20.wai2k.events.LogisticsSupportReceivedEvent
import com.waicool20.wai2k.events.LogisticsSupportSentEvent
import com.waicool20.wai2k.game.GFL
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.util.readText
import com.waicool20.waicoolutils.firstAsync
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class Navigator(
    override val scriptRunner: ScriptRunner,
    override val region: AndroidRegion,
    override val config: Wai2kConfig,
    override val profile: Wai2kProfile
) : ScriptComponent {
    private val logger = loggerFor<Navigator>()
    private val gameState get() = scriptRunner.gameState
    private val scriptStats get() = scriptRunner.scriptStats

    companion object {
        private val transitionDelays = LinkedList<Long>()
        private var restartCounter = 0
    }

    /**
     * Finds the current location
     *
     * @return Current [GameLocation]
     */
    suspend fun identifyCurrentLocation(retries: Int = 5): GameLocation {
        if (!isGameActive()) restartGameInternal(0)
        logger.info("Identifying current location")
        val start = System.currentTimeMillis()
        val locations = locations.entries.map { it.value }
        repeat(retries) { i ->
            checkLogistics(true)
            checkAutoBattle()
            val r = region.asCachedRegion()
            val l = locations.firstAsync { it.isInRegion(r) }
            if (l == null) {
                logger.warn("Could not find location after ${i + 1} attempts, retries remaining: ${retries - i - 1}")
                delay(1000L * (i + 1))
            } else {
                logger.info("At ${l.id}, took ${System.currentTimeMillis() - start} ms")
                return l
            }
        }
        throw UnknownLocationException()
    }

    /**
     * Attempts to navigate to the destination
     *
     * @param destination Name of destination
     */
    suspend fun navigateTo(destination: LocationId, retries: Int = 3) {
        retry@ for (r in 0 until retries) {
            val finalDest = locations[destination] ?: throw InvalidDestinationException(destination)
            logger.info("Navigating to ${finalDest.id}")
            val cLocation = gameState.currentGameLocation.takeIf { it.isInRegion(region) }
                ?: identifyCurrentLocation()
            val path = cLocation.shortestPathTo(finalDest)
            if (path.isEmpty()) {
                logger.info("Already at ${finalDest.id}")
                return
            }
            logger.debug("Found solution: CURRENT(${cLocation.id})->${path.formatted()}")
            for ((srcLoc, destLoc, link) in path) {
                if (link.skippable) {
                    logger.info(
                        "At ${destLoc.id} | ${
                            path.dropWhile { it.dest != destLoc }.formatted()
                        }"
                    )
                    continue
                }
                logger.info("Going to ${destLoc.id}")
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
                        link.asset.getSubRegionFor(region).apply {
                            // Shrink region slightly to 90% of defined size
                            grow((width * -0.1).roundToInt(), (height * -0.1).roundToInt())
                        }.click()
                        delay(avgTransitionDelay + config.scriptConfig.baseNavigationDelay)
                        ticks++
                    }
                    val ntdStart = System.currentTimeMillis()
                    if (!srcLoc.isInRegion(region) || destLoc.isInRegion(region)) break
                    noneTransitionDelay += System.currentTimeMillis() - ntdStart
                    if (checkLogistics()) skipTransitionDelay = true
                }

                logger.info("Waiting for transition to ${destLoc.id}")
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

                // Calculate the transition delays and delay coefficients
                // Update the restart counter as needed
                val transitionTime = System.currentTimeMillis() - startTransitionTime
                if (!skipTransitionDelay) {
                    transitionDelays.add(transitionTime - avgTransitionDelay * ticks - noneTransitionDelay)
                    if (transitionDelays.size >= 20) transitionDelays.removeFirst()
                }
                gameState.delayCoefficient =
                    avgTransitionDelay.toDouble() / config.gameRestartConfig.averageDelay
                if (gameState.delayCoefficient > config.gameRestartConfig.delayCoefficientThreshold) {
                    restartCounter++
                } else {
                    if (restartCounter > 0) restartCounter--
                }

                updateAverageDelay(avgTransitionDelay)
                updateRestartFlag()
                logger.info(
                    "Transition: $transitionTime ms" +
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
                    logger.info(
                        "At ${destLoc.id} | ${
                            path.dropWhile { it.dest != destLoc }.formatted()
                        }"
                    )
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

    private val logisticWaitList =
        listOf(LocationId.HOME, LocationId.HOME_STATUS, LocationId.UNKNOWN)

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
                .none { Duration.between(Instant.now(), it.eta).seconds <= 15 }
        ) return false
        var logisticsArrived = false
        loop@ while (true) {
            when {
                region.has(FileTemplate("navigator/logistics_arrived.png")) -> {
                    logger.info("An echelon has arrived from logistics")
                    region.click()
                }
                ocr.readText(region.subRegion(575, 425, 1000, 100))
                    .contains("Repeat") -> {
                    // Even if the logistics arrived didnt show up, its possible
                    // that it was clicked through by some other function
                    logger.info("An echelon has arrived from logistics, but already at repeat dialog for some reason...")
                }
                else -> break@loop
            }
            delay(500)

            logisticsArrived = true

            // Continue based on receival mode
            val cont = when (profile.logistics.receiveMode) {
                Wai2kProfile.Logistics.ReceivalMode.ALWAYS_CONTINUE -> {
                    logger.info("Continuing this logistics support")
                    true
                }
                Wai2kProfile.Logistics.ReceivalMode.RANDOM -> {
                    if (Random().nextBoolean()) {
                        logger.info("Randomized receive, continue logistics support this time")
                        true
                    } else {
                        logger.info("Randomized receive, stopping logistics support this time")
                        false
                    }
                }
                Wai2kProfile.Logistics.ReceivalMode.ALWAYS_CANCEL -> {
                    logger.info("Stopping this logistics support")
                    false
                }
                null -> {
                    logger.info("Defaulting to continue")
                    true
                }
            }
            EventBus.publish(LogisticsSupportReceivedEvent(sessionId, elapsedTime))
            val image = if (cont) {
                EventBus.publish(LogisticsSupportSentEvent(sessionId, elapsedTime))
                "ok.png"
            } else "cancel-logi.png"

            region.waitHas(FileTemplate(image), 10000)?.click()


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
     * Checks if any auto battle echelons arrived and continues them
     */
    suspend fun checkAutoBattle() {
        if (!profile.autoBattle.enabled) return
        while (true) {
            if (region.subRegion(138, 740, 110, 100)
                    .has(FileTemplate("navigator/autobattle_arrived.png"))
            ) {
                logger.info("An echelon has arrived from auto battle")
                // Dismiss mission accomplished and rewards
                repeat(2) {
                    region.click()
                    delay(500)
                }
                logger.info("Continuing this auto battle")
                region.subRegion(1432, 822, 336, 134)
                    .clickTemplateWhile(FileTemplate("navigator/autobattle_repeat.png")) { has(it) }
                logger.info("Waiting a bit to see if anymore echelons arrive")
                delay(5000)
            } else break
        }
    }

    /**
     * Checks for the gamestate restart flag and restarts the game if required
     * This assumes that automatic login is enabled and no updates are required
     */
    suspend fun checkRequiresRestart() {
        if (config.gameRestartConfig.enabled && gameState.requiresRestart) {
            restartGame("Game is slowing down")
            restartCounter = 0
            transitionDelays.clear()
        }
    }

    /**
     * Restarts the game
     * This assumes that automatic login is enabled and no updates are required
     */
    suspend fun restartGame(reason: String) {
        if (scriptStats.gameRestarts >= config.gameRestartConfig.maxRestarts) {
            logger.info("Maximum of restarts reached, terminating script instead")
            scriptRunner.stop("Max restarts reached")
        }
        gameState.requiresRestart = false
        EventBus.publish(GameRestartEvent(reason, profile.combat.map, sessionId, elapsedTime))
        logger.info("Game will now restart")
        restartGameInternal()
    }

    private suspend fun restartGameInternal(delay: Long = 5000) {
        ProcessManager(region.device).restart(GFL.PKG_NAME, delay)
        logger.info("Game started, waiting for login screen")
        while (!locations.getValue(LocationId.GAME_START).isInRegion(region)) delay(5000)
        logger.info("Logging in")
        delay(1000)
        val login = region.subRegion(200, 19, 96, 87)
        while (coroutineContext.isActive) {
            if (locations.getValue(LocationId.GAME_START).isInRegion(region)) {
                region.subRegion(630, 400, 900, 300).click()
                delay(10000)
            }
            checkLogistics()
            // Check for sign in or achievement popup
            if (region.subRegion(396, 244, 80, 80).has(FileTemplate("home-popup.png"))) {
                logger.info("Detected popup, dismissing...")
                repeat(2) { region.subRegion(2017, 151, 129, 733).click() }
            }
            // Check for daily login
            if (login.has(FileTemplate("home-popup1.png"))) {
                logger.info("Detected daily login/event screen, dismissing...")
                login.click()
            }
            region.subRegion(900, 720, 350, 185)
                .findBest(FileTemplate("close.png"))?.region?.click()
            if (locations.getValue(LocationId.HOME).isInRegion(region)) {
                logger.info("Logged in, waiting for 10s to see if anything happens")
                delay(10_000)
                if (locations.getValue(LocationId.HOME).isInRegion(region)) {
                    gameState.currentGameLocation = locations.getValue(LocationId.HOME)
                    break
                }
            }
            delay(1000)
        }
        logger.info("Finished logging in")
        gameState.signalRestart()
    }

    private fun List<GameLocation.GameLocationLink>?.formatted(): String {
        return this?.joinToString("->") { "${it.dest.id}" } ?: ""
    }

    private fun isGameActive(): Boolean {
        return ProcessManager(region.device).currentActivity.contains(GFL.PKG_NAME)
    }
}
