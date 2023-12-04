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


import com.waicool20.cvauto.core.template.FT
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.events.LogisticsSupportSentEvent
import com.waicool20.wai2k.game.Echelon
import com.waicool20.wai2k.game.LogisticsSupport
import com.waicool20.wai2k.game.location.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.util.formatted
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.wai2k.util.readText
import com.waicool20.wai2k.util.useCharFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import kotlin.coroutines.coroutineContext

@Suppress("unused")
class LogisticsSupportModule(navigator: Navigator) : ScriptModule(navigator) {
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
        if (logisticSupportLimitReached()) return
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
        if (queue.isEmpty()) return
        navigator.navigateTo(LocationId.LOGISTICS_SUPPORT)
        delay(2000)
        if (checkDispatched()) return
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
                .shuffled().firstOrNull()
            if (assignment != null) dispatchEchelon(echelon, assignment)
        }
    }

    private suspend fun checkDispatched(): Boolean {
        logger.info("Checking dispatched count")
        while (coroutineContext.isActive) {
            val n = ocr.useCharFilter("0123456789/")
                .readText(region.subRegion(1737, 150, 85, 55), threshold = 0.5, invert = true)
                .also { logger.debug("Dispatch count ocr: $it") }
                .take(1)
                .toIntOrNull() ?: continue
            logger.info("$n echelons dispatched")
            if (n >= 4) {
                // If we get to this point, then the timer has expired, but we haven't
                // re-dispatched the echelon
                logger.info("Timer expired, but detected 4 dispatched echelons, going back home to check")
                navigator.navigateTo(LocationId.HOME)
                delay(2000)
                gameState.requiresUpdate = true
                return true
            }
            return false
        }
        return false
    }

    /**
     * Tries to return to home and receive echelons if any logistic timers have expired
     */
    private suspend fun checkLogisticTimers() {
        if (gameState.echelons.any { it.logisticsSupportAssignment?.eta?.isBefore(Instant.now()) == true }) {
            logger.info("An echelon probably came back, gonna check home")
            navigator.navigateTo(LocationId.HOME)
            navigator.checkLogistics()
            gameState.requiresUpdate = true
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
        delay(500)
        val missionIndex = (nextMission.number - 1) % 4

        if (missionRunning(missionIndex)) {
            logger.info("An echelon is already running that mission, maybe ocr failed when updating game state")
            gameState.requiresUpdate = true
            return
        }

        // Start mission
        clickMissionStart(missionIndex)

        delay(250)

        // Click the ok button of the popup if any of the resources broke the hard cap
        // Use subregion, so it doesn't click dispatch ok instead
        region.subRegion(223, 191, 1372, 716).findBest(FT("ok.png"))
            ?.also { logger.info("One of the resources reached its limit!") }
            ?.region?.click()

        region.waitHas(FT("logistics/formation.png"), 10000)
        if (!echelon.clickEchelon(this, 50)) {
            region.findBest(FT("cancel-deploy.png"))?.region?.click()
            return
        }
        // Click ok button
        delay(300)

        region.clickTemplateWhile(FT("ok.png")) { has(it) }

        // Wait for logistics mission icon to appear again
        region.waitHas(FT("logistics/logistics.png"), 7000)

        // Check if mission is running
        if (missionRunning(missionIndex)) {
            // Update eta
            val eta = Instant.now() + nextMission.duration
            echelon.logisticsSupportAssignment = LogisticsSupport.Assignment(nextMission, eta)
            logger.info("Dispatched $echelon to logistic support ${nextMission.formattedString}, ETA: ${eta.formatted()}")
            EventBus.publish(LogisticsSupportSentEvent(sessionId, elapsedTime))
            return
        }
        // Mission not running, requirements not met
        logger.info("Logistic support failed to dispatch, are requirements being met?")
        logger.info("Disabled logistics support for echelon ${echelon.number}")
        delay(300)
        // Click close button
        region.subRegion(940, 757, 280, 107).click()
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
        clickChapter(ls.chapter)
        logger.info("At logistics support chapter ${ls.chapter}")
    }

    /**
     * Checks if the mission for given index is running
     *
     * @param mission Index of mission from left to right 0-3
     */
    private fun missionRunning(mission: Int): Boolean {
        val missionRegion = region.subRegion(585 + (333 * mission), 788, 305, 280)
        return missionRegion.has(FT("logistics/retreat.png"))
    }

    /**
     * Clicks on mission start button for given mission index
     *
     * @param mission Index of mission from left to right 0-3
     */
    private suspend fun clickMissionStart(mission: Int) {
        logger.debug("Opening up the logistic support menu")
        // Left most mission button x: 704 y: 219 w: 306 h: 856
        val missionRegion = region.subRegion(585 + (333 * mission), 214, 305, 855)
        // Need a separate check region because the ammo icon might not be covered by the resource limit popup
        val checkRegion = region.subRegion(630, 418, 85, 120)
        missionRegion.clickWhile { checkRegion.has(FT("logistics/ammo.png")) }
    }

    /**
     * Checks if the amount of ongoing logistic support is 4
     */
    private fun logisticSupportLimitReached(): Boolean {
        return gameState.echelons.count { it.logisticsSupportAssignment?.eta?.isAfter(Instant.now()) == true } >= 4
    }
}
