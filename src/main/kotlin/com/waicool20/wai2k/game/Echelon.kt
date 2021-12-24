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

package com.waicool20.wai2k.game

import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.util.digitsOnly
import com.waicool20.wai2k.util.readText
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.mapAsync
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import kotlin.coroutines.coroutineContext

data class Echelon(val number: Int) {
    private val logger = loggerFor<Echelon>()
    var logisticsSupportEnabled = true
    var logisticsSupportAssignment: LogisticsSupport.Assignment? = null
    val members: List<Member> = List(5) { Member(it + 1) }

    data class Member(val number: Int) {
        var name: String = "Unknown"
        var needsRepair: Boolean = false
        var repairEta: Instant? = null
    }

    fun hasRepairs() = members.any { it.repairEta != null }
    fun needsRepairs() = members.any { it.needsRepair }

    /**
     * Clicks this echelon at the echelon list
     *
     * @param xOffset The x coordinate of the region where the rects of the echelons appear
     */
    suspend fun clickEchelon(sc: ScriptComponent, xOffset: Int): Boolean = with(sc) {
        logger.debug("Clicking the echelon")
        val eRegion = region.subRegion(xOffset, 40, 170, region.height - 140)
        delay(100)

        val start = System.currentTimeMillis()
        while (coroutineContext.isActive) {
            val echelons = eRegion.findBest(FileTemplate("echelons/echelon.png"), 8)
                .map { it.region }
                .map { it.copy(it.x + it.width, it.y - 40, 70, 95) }
                .mapAsync {
                    val n = ocr.digitsOnly().readText(it, scale = 0.5)
                        .replace("18", "10").toIntOrNull() ?: return@mapAsync null
                    n to it
                }.filterNotNull()
                .toMap()
            logger.debug("Visible echelons: ${echelons.keys}")
            when {
                echelons.keys.isEmpty() -> {
                    logger.info("No echelons available...")
                    return false
                }
                number in echelons.keys -> {
                    logger.info("Found echelon!")
                    echelons[number]?.click()
                    return true
                }
            }
            val lEchelon = echelons.keys.minOrNull() ?: echelons.keys.firstOrNull() ?: continue
            val hEchelon = echelons.keys.maxOrNull() ?: echelons.keys.lastOrNull() ?: continue
            val lEchelonRegion = echelons[lEchelon] ?: continue
            val hEchelonRegion = echelons[hEchelon] ?: continue
            when {
                number <= lEchelon -> {
                    logger.debug("Swiping down the echelons")
                    lEchelonRegion.swipeTo(hEchelonRegion)
                }
                number >= hEchelon -> {
                    logger.debug("Swiping up the echelons")
                    hEchelonRegion.swipeTo(lEchelonRegion)
                }
            }
            delay(2000)
            if (System.currentTimeMillis() - start > 45000) {
                scriptRunner.gameState.requiresUpdate = true
                logger.warn("Failed to find echelon, maybe ocr failed?")
                break
            }
        }
        return false
    }
}

