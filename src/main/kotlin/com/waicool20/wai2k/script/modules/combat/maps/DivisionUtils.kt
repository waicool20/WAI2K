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

package com.waicool20.wai2k.script.modules.combat.maps

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlin.random.Random

object DivisionUtils {
    private val logger = loggerFor<DivisionUtils>()

    /*
     * Pretty much everything farmed here will be infinity, which has no difficulty select
     */
    suspend fun setDifficulty(sc: ScriptComponent) {
        val ocrRegion = sc.region.subRegion(211, 980, 105, 50)
        val target = sc::class.simpleName!!.endsWith("EX")
        logger.info("Checking difficulty")
        val current =
            Ocr.forConfig(sc.config).doOCRAndTrim(ocrRegion).contains("Hard", ignoreCase = true)
        if (current != target) {
            logger.info("Changing difficulty")
            ocrRegion.click()
            delay(500)
        }
    }

    /*
     * Move the Division map select screen to the bottom left
     */
    suspend fun panBottomLeft(sc: ScriptComponent) {
        val r = sc.region.subRegionAs<AndroidRegion>(540, 900, 125, 125)

        logger.info("Pinch")
        sc.region.pinch(
            Random.nextInt(820, 840),
            Random.nextInt(920, 970),
            45.0,
            500
        )
        delay(400)

        logger.info("Pan down left")
        repeat(3) {
            r.swipeTo(r.copy(y = 200, x = r.x + 125), 500)
            delay(250)
        }
    }

    /*
     * Attempts to set the 'x' for map select so that enterMap() can click a consistent region
     */
    suspend fun scrollRight(sc: ScriptComponent, times: Int) {
        // Generic maprunner.pinch() type stuff pls
        var nTimes = times
        val pad = Random.nextInt(0, 50)
        val r = sc.region.subRegionAs<AndroidRegion>(1800, 900, 5, 50)

        logger.info("Panning right $times times")
        while (nTimes > 0) {
            r.swipeTo(r.copy(x = r.x - (1000 + pad), y = r.y + 50), 500)
            delay(250)
            nTimes--
        }
        logger.info("Adjusting for padding")
        r.swipeTo(r.copy(x = r.x + (pad * times)), 500)
        delay(500)
    }
}