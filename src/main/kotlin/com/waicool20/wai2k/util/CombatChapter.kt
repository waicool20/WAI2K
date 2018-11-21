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
 * GNU General Public License for more detai
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.waicool20.wai2k.util

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.waicoolutils.filterAsync
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay

object CombatChapter {
    private val logger = loggerFor<CombatChapter>()

    /**
     * Clicks given chapter, only works if already on Combat or Logistic Support screen
     */
    suspend fun clickChapter(chapter: Int, region: AndroidRegion) {
        // Region containing all chapters
        val cRegion = region.subRegion(407, 146, 283, 934)
        // Top 1/4 part of lsRegion
        val upperSwipeRegion = cRegion.subRegion(cRegion.w / 2 - 15, 0, 30, cRegion.h / 4)
        // Lower 1/4 part of lsRegion
        val lowerSwipeRegion = cRegion.subRegion(cRegion.w / 2 - 15, cRegion.h / 4 + cRegion.h / 2, 30, cRegion.h / 4)
        val cSimilarity = 0.9
        while (cRegion.doesntHave("chapters/$chapter.png", cSimilarity)) {
            delay(100)
            val chapters = (0..7).filterAsync { cRegion.has("chapters/$it.png", cSimilarity) }
            logger.debug("Visible chapters: $chapters")
            when {
                chapter <= chapters.min() ?: 3 -> {
                    logger.debug("Swiping down the chapters")
                    upperSwipeRegion.swipeToRandomly(lowerSwipeRegion)
                }
                chapter >= chapters.max() ?: 4 -> {
                    logger.debug("Swiping up the chapters")
                    lowerSwipeRegion.swipeToRandomly(upperSwipeRegion)
                }
            }
            delay(300)
        }
        cRegion.subRegion(0, 0, 195, cRegion.h)
                .clickUntilGone("chapters/clickable/$chapter.png", 20, 0.96)
    }
}