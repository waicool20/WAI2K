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
import com.waicool20.wai2k.game.GameState
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.game.LogisticsSupport
import com.waicool20.wai2k.game.LogisticsSupport.Assignment
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCR
import com.waicool20.waicoolutils.DurationUtils
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.experimental.async
import java.time.LocalDateTime

class InitModule(
        gameState: GameState,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(gameState, region, config, profile, navigator) {
    private val logger = loggerFor<InitModule>()
    override suspend fun execute() {
        navigator.navigateTo(LocationId.HOME_STATUS)
        updateLogistics()
        logger.info("Finished updating game state")
    }

    private suspend fun updateLogistics() {
        logger.info("Reading logistics support status")
        region.subRegion(347, 0, 229, region.h).findAllOrEmpty("init/logistics.png")
                // Map each region to whole logistic support entry
                .map { region.subRegion(it.x - 143, it.y - 22, 1088, 144) }
                .map {
                    async {
                        // Echelon section on the right without the word "Echelon"
                        Ocr.forConfig(config).doOCR(it.subRegion(0, 25, 83, 119))
                    } to async {
                        // Brown region containing "In logistics x-x xx:xx:xx"
                        Ocr.forConfig(config).doOCR(it.subRegion(114, 22, 859, 108))
                    }
                }
                .map { "${it.first.await()} ${it.second.await()}" }
                .mapNotNull {
                    Regex("(\\d) In logistics (\\d) - (\\d) (\\d\\d):(\\d\\d):(\\d\\d)").matchEntire(it)?.destructured
                }.forEach { (sEchelon, sChapter, sNumber, sHour, sMinutes, sSeconds) ->
                    val echelon = sEchelon.toInt()
                    val logisticsSupport = LogisticsSupport.list[sChapter.toInt() * 4 + sNumber.toInt() - 1]
                    val duration = DurationUtils.of(sSeconds.toLong(), sMinutes.toLong(), sHour.toLong())
                    val eta = LocalDateTime.now() + duration
                    logger.info("Echelon $echelon is doing logistics support ${logisticsSupport.number}, ETA: $eta")
                    gameState.echelons[echelon - 1].logisticsSupportAssignment =
                            Assignment(logisticsSupport, eta)
                }
    }
}