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
import com.waicool20.wai2k.game.Echelon
import com.waicool20.wai2k.game.GameState
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.game.LogisticsSupport
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptStats
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.wai2k.util.formatted
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.yield
import java.time.Instant
import kotlin.coroutines.experimental.coroutineContext

class LogisticsSupportModule(
        scriptStats: ScriptStats,
        gameState: GameState,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(scriptStats, gameState, region, config, profile, navigator) {
    private val logger = loggerFor<LogisticsSupportModule>()
    override suspend fun execute() {
        if (!profile.logistics.enabled) return
        checkAndDispatchEchelons()
        checkLogisticTimers()
    }

    /**
     * Checks if any echelons require dispatching then dispatches them
     */
    private suspend fun checkAndDispatchEchelons() {
        val queue = profile.logistics.assignments
                // Map to echelon
                .mapKeys { gameState.echelons[it.key - 1] }
                // Keep all echelons that don't have an assignment already
                .filter { it.key.logisticsSupportAssignment == null }
                // Remove all echelons with disabled logistics
                .filter { it.key.logisticsSupportEnabled }
                // Keep all echelons that have configured assignments
                .filter { it.value.isNotEmpty() }
                // Remove all the echelons that have repairs ongoing
                .filterNot { it.key.hasRepairs() }
        // Return if no echelons to dispatch or logistic support 4/4 was reached
        if (queue.isEmpty() || logisticSupportLimitReached()) return
        navigator.navigateTo(LocationId.LOGISTICS_SUPPORT)
        queue.entries.shuffled().forEach { (echelon, ls) ->
            if (logisticSupportLimitReached()) {
                logger.info("4 logistic support have been dispatched, ignoring further assignments")
                return
            }
            logger.info("Valid assignments for $echelon: ${ls.joinToString()}")
            val assignment = LogisticsSupport.list
                    .filter { ls.contains(it.number) }
                    // Remove any logistic support that are being run by other echelons atm
                    .filter { l -> gameState.echelons.none { it.logisticsSupportAssignment?.logisticSupport == l } }
                    // Choose a random logistic support
                    .shuffled().firstOrNull() ?: return
            dispatchEchelon(echelon, assignment)
            yield()
        }
    }

    /**
     * Tries to return to home and receive echelons if any logistic timers have expired
     */
    private suspend fun checkLogisticTimers() {
        if (gameState.echelons.any { it.logisticsSupportAssignment?.eta?.isBefore(Instant.now()) == true }) {
            logger.info("An echelon probably came back, gonna check home")
            navigator.navigateTo(LocationId.HOME)
        }
    }

    /**
     * Dispatches an echelon to some logistic support
     *
     * @param echelon Echelon to dispatch
     * @param nextMission Next Logistic Support mission to dispatch the echelon to
     */
    private suspend fun dispatchEchelon(echelon: Echelon, nextMission: LogisticsSupport) {
        logger.info("Next mission for $echelon is ${nextMission.number}")
        clickLogisticSupportChapter(nextMission)
        val missionIndex = (nextMission.number - 1) % 4

        if (missionRunning(missionIndex)) {
            logger.info("An echelon is already running that mission, maybe ocr failed when updating game state")
            gameState.requiresUpdate = true
            return
        }

        // Start mission
        logger.debug("Opening up the logistic support menu")
        clickMissionStart(missionIndex)
        region.waitSuspending("logistics/formation.png", 10)
        logger.debug("Clicking the echelon")
        clickEchelon(echelon)
        // Click ok button
        delay(300)

        region.clickUntilGone("logistics/ok.png", 10)

        // Wait for logistics mission icon to appear again
        region.subRegion(131, 306, 257, 118).waitSuspending("logistics/logistics.png", 7)

        // Check if mission is running
        if (missionRunning(missionIndex)) {
            // Update eta
            val eta = Instant.now() + nextMission.duration
            echelon.logisticsSupportAssignment = LogisticsSupport.Assignment(nextMission, eta)
            logger.info("Dispatched $echelon to logistic support ${nextMission.number}, ETA: ${eta.formatted()}")
            scriptStats.logisticsSupportSent++
            return
        }
        // Mission not running, requirements not met
        logger.info("Logistic support failed to dispatch, are requirements being met?")
        logger.info("Disabled logistics support for echelon ${echelon.number}")
        delay(300)
        // Click close button
        region.subRegion(940, 757, 280, 107).clickRandomly()
        // Disable echelon
        echelon.logisticsSupportEnabled = false
    }

    /**
     * Clicks the chapter of the given logistic support
     *
     * @param ls Logistic support
     */
    private suspend fun clickLogisticSupportChapter(ls: LogisticsSupport) {
        logger.info("Choosing logistics support chapter ${ls.chapter}")
        val lsRegion = region.subRegion(407, 0, 283, region.h)
        val upperSwipeRegion = region.subRegion(407, 154, 192, 439)
        val lowerSwipeRegion = region.subRegion(407, 612, 192, 439)

        var retries = 0
        while (lsRegion.doesntHave("chapters/${ls.chapter}.png") && retries++ < 5) {
            delay(100)
            val currentLowestChapter = (0..6).firstOrNull { lsRegion.has("chapters/$it.png") } ?: 3
            val currentHighestChapter = (6 downTo 0).firstOrNull { lsRegion.has("chapters/$it.png") }
                    ?: 3
            when {
                ls.chapter < currentLowestChapter -> {
                    logger.debug("Swiping down the chapters")
                    upperSwipeRegion.swipeToRandomly(lowerSwipeRegion)
                }
                ls.chapter > currentHighestChapter -> {
                    logger.debug("Swiping up the chapters")
                    lowerSwipeRegion.swipeToRandomly(upperSwipeRegion)
                }
            }
            delay(300)
        }

        if (lsRegion.has("chapters/${ls.chapter}.png")) {
            logger.debug("Found the chapter, clicking it...")
            lsRegion.findOrNull("chapters/clickable/${ls.chapter}.png")?.clickRandomly()
            delay(200)
        } else {
            logger.warn("Couldn't find the chapter")
            coroutineContext.cancelAndYield()
        }
    }

    /**
     * Checks if the mission for given index is running
     *
     * @param mission Index of mission from left to right 0-3
     */
    private fun missionRunning(mission: Int): Boolean {
        val missionRegion = region.subRegion(704 + (333 * mission), 219, 306, 856)
        return missionRegion.has("logistics/retreat.png")
    }

    /**
     * Clicks on mission start button for given mission index
     *
     * @param mission Index of mission from left to right 0-3
     */
    private suspend fun clickMissionStart(mission: Int) {
        // Left most mission button x: 704 y: 219 w: 306 h: 856
        val missionRegion = region.subRegion(704 + (333 * mission), 219, 306, 856)
        while (missionRegion.has("logistics/by.png")) {
            missionRegion.clickRandomly(); yield()
        }
    }

    /**
     * Clicks on the echelon when in the dispatch screen
     *
     * @param echelon Echelon to click
     */
    private suspend fun clickEchelon(echelon: Echelon) {
        region.subRegion(120, 0, 183, region.h)
                .findOrNull("logistics/echelon${echelon.number}.png")?.clickRandomly()
        yield()
    }

    /**
     * Checks if the amount of ongoing logistic support is 4
     */
    private fun logisticSupportLimitReached(): Boolean {
        return gameState.echelons.mapNotNull { it.logisticsSupportAssignment }.size >= 4
    }
}