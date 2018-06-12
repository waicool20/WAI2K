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
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCR
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.yield
import java.time.ZonedDateTime

class LogisticsSupportModule(
        gameState: GameState,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(gameState, region, config, profile, navigator) {
    private val logger = loggerFor<LogisticsSupportModule>()
    override suspend fun execute() {
        if (!profile.logistics.enabled) return
        val queue = profile.logistics.assignments.mapKeys { gameState.echelons[it.key - 1] }
                // Keep all echelons that don't have an assignment already
                .filter { it.key.logisticsSupportAssignment == null }
                // Remove all echelons with disable logistics
                .filterNot { it.key.logisticsSupportAssignment?.logisticSupport == LogisticsSupport.DISABLED }
                // Keep all echelons that have configured assignments
                .filter { it.value.isNotEmpty() }
                // Remove all the echelons that have repairs ongoing
                .filterNot { it.key.hasRepairs() }
        // Return if no echelons to dispatch
        if (queue.isEmpty()) return
        navigator.navigateTo(LocationId.LOGISTICS_SUPPORT)
        queue.forEach { (echelon, ls) ->
            logger.info("Valid assignments for $echelon: ${ls.joinToString()}")
            dispatchEchelon(echelon, LogisticsSupport.list.filter { ls.contains(it.number) }.shuffled().first())
            yield()
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
        navigator.navigateTo(nextMission.locationId)
        val missionIndex = (nextMission.number - 1) % 4
        clickMissionStart(missionIndex)
        clickEchelon(echelon)

        // Click ok button
        delay(200)
        region.subRegion(1761, 910, 251, 96).clickRandomly()
        delay(200)

        // Check if mission is running
        if (missionRunning(missionIndex)) {
            // Update eta
            val eta = ZonedDateTime.now() + nextMission.duration
            echelon.logisticsSupportAssignment = LogisticsSupport.Assignment(nextMission, eta)
            logger.info("Dispatched $echelon to logistic support ${nextMission.number}, ETA: $eta")
            return
        }
        // Mission not running, requirements not met
        logger.info("Logistic support failed to dispatch, are requirements being met?")
        logger.info("Disabled logistics support for echelon ${echelon.number}")
        delay(300)
        // Click close button
        region.subRegion(940, 757, 280, 107).clickRandomly()
        // Disable echelon
        echelon.logisticsSupportAssignment = LogisticsSupport.Assignment(LogisticsSupport.DISABLED, ZonedDateTime.now())
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
        missionRegion.clickRandomly(); yield()
    }

    /**
     * Clicks on the echelon when in the dispatch screen
     *
     * @param echelon Echelon to click
     */
    private suspend fun clickEchelon(echelon: Echelon) {
        val map = region.subRegion(120, 0, 183, region.h).findAllOrEmpty("logistics/echelon.png")
                .map { region.subRegion(it.x - 8, it.y, 183, 108) }
                .associate {
                    async {
                        Ocr.forConfig(config).doOCR(it.subRegion(115, 0, 68, it.h))
                    } to it
                }.mapKeys {
                    it.key.await().let { no ->
                        no.filter { it.isDigit() }.toIntOrNull()
                                ?: Ocr.cleanNumericString(no).filter { it.isDigit() }.toIntOrNull()
                    }
                }
        logger.info("Found echelons ${map.keys}")
        logger.info("Choosing echelon ${echelon.number}")
        map[echelon.number]?.clickRandomly() ?: error("Could not choose echelon ${echelon.number}")
    }
}