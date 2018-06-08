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
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.yield
import java.time.LocalDateTime

class LogisticsSupportModule(
        gameState: GameState,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(gameState, region, config, profile, navigator) {
    private val logger = loggerFor<LogisticsSupportModule>()
    override suspend fun execute() {
        navigator.navigateTo(LocationId.LOGISTICS_SUPPORT)
        profile.logistics.assignments.mapKeys { gameState.echelons[it.key - 1] }.forEach { (echelon, ls) ->
            echelon.logisticsSupportAssignment = null //TODO remove debug
            if (echelon.logisticsSupportAssignment == null && ls.isNotEmpty()) {
                logger.info("Valid assignments for $echelon: ${ls.joinToString()}")
                dispatchEchelon(echelon, LogisticsSupport.list.filter { ls.contains(it.number) }.shuffled().first())
            }
            yield()
        }
    }

    private suspend fun dispatchEchelon(echelon: Echelon, nextMission: LogisticsSupport) {
        logger.info("Next mission for $echelon is ${nextMission.number}")
        navigator.navigateTo(nextMission.locationId)
        delay(500)
        clickMissionStart((nextMission.number - 1) % 4)
        //TODO clickEchelon(echelon)

        // Update eta
        val eta = LocalDateTime.now() + nextMission.duration
        echelon.logisticsSupportAssignment = LogisticsSupport.Assignment(nextMission, eta)
        logger.info("Dispatched $echelon to logistic support ${nextMission.number}, ETA: $eta")
    }

    private suspend fun clickMissionStart(mission: Int) {
        // Left most mission button x: 730 y: 934 w: 254 h: 116
        val missionRegion = region.subRegion(730 + (333 * mission), 934, 254, 116)
        missionRegion.clickRandomly(); yield()
    }
}