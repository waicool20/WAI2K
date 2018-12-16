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
import com.waicool20.wai2k.game.DollFilterRegions
import com.waicool20.wai2k.game.DollType
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.waicoolutils.filterAsync
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext

abstract class ScriptModule(
        protected val scriptRunner: ScriptRunner,
        protected val region: AndroidRegion,
        protected val config: Wai2KConfig,
        protected val profile: Wai2KProfile,
        protected val navigator: Navigator
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = scriptRunner.coroutineContext

    private val logger = loggerFor<ScriptModule>()

    val gameState get() = scriptRunner.gameState
    val scriptStats get() = scriptRunner.scriptStats

    abstract suspend fun execute()

    protected val dollFilterRegions by lazy { DollFilterRegions(region) }

    /**
     * Applies the given corresponding doll filters, only works when the filter button
     * is on screen. ( eg. Formation/Enhancement/Disassemble screens )
     *
     * @param stars No. of stars, null if you don't care (Default)
     * @param type Doll type, null if you don't care (Default)
     * @param reset Resets filters first before applying the filters
     */
    protected suspend fun applyDollFilters(stars: Int? = null, type: DollType? = null, reset: Boolean = false) {
        if (stars == null && type == null && !reset) return
        region.mouseDelay(0.0) {
            dollFilterRegions.filter.clickRandomly()
            delay(500)

            if (reset) {
                logger.info("Resetting filters")
                dollFilterRegions.reset.clickRandomly(); yield()
                dollFilterRegions.filter.clickRandomly()
                delay(500)
            }
            if (stars != null) {
                logger.info("Applying $stars stars filter")
                dollFilterRegions.starRegions[stars]?.clickRandomly(); yield()
            }
            if (type != null) {
                logger.info("Applying $type doll type filter")
                dollFilterRegions.typeRegions[type]?.clickRandomly(); yield()
            }

            logger.info("Confirming filters")
            dollFilterRegions.confirm.clickRandomly(); yield()
        }
    }

    /**
     * Clicks given chapter, only works if already on Combat or Logistic Support screen
     *
     * @param chapter Chapter number
     */
    protected suspend fun clickChapter(chapter: Int) {
        val CHAPTER_SIMILARITY = 0.9
        // Region containing all chapters
        val cRegion = region.subRegion(407, 146, 283, 934)
        // Top 1/4 part of lsRegion
        val upperSwipeRegion = cRegion.subRegion(cRegion.w / 2 - 15, 0, 30, cRegion.h / 4)
        // Lower 1/4 part of lsRegion
        val lowerSwipeRegion = cRegion.subRegion(cRegion.w / 2 - 15, cRegion.h / 4 + cRegion.h / 2, 30, cRegion.h / 4)
        while (cRegion.doesntHave("chapters/$chapter.png", CHAPTER_SIMILARITY)) {
            navigator.checkLogistics()
            val chapters = (0..7).filterAsync { cRegion.has("chapters/$it.png", CHAPTER_SIMILARITY) }
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
        navigator.checkLogistics()
        cRegion.subRegion(0, 0, 195, cRegion.h)
                .clickUntilGone("chapters/clickable/$chapter.png", 20, 0.96)
    }
}