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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.DollFilterRegions
import com.waicool20.wai2k.game.DollType
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.cancelAndYield
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

    companion object {
        private const val CHAPTER_MIN = 0
        private const val CHAPTER_MAX = 9
        private const val CHAPTER_SIMILARITY = 0.9
    }

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
        dollFilterRegions.filter.click()
        delay(500)

        if (reset) {
            logger.info("Resetting filters")
            dollFilterRegions.reset.click(); yield()
            dollFilterRegions.filter.click()
            delay(500)
        }
        if (stars != null) {
            logger.info("Applying $stars stars filter")
            dollFilterRegions.starRegions[stars]?.click(); yield()
        }
        if (type != null) {
            logger.info("Applying $type doll type filter")
            dollFilterRegions.typeRegions[type]?.click(); yield()
        }

        logger.info("Confirming filters")
        dollFilterRegions.confirm.click(); yield()
    }

    /**
     * Clicks given chapter, only works if already on Combat or Logistic Support screen
     *
     * @param chapter Chapter number
     */
    protected suspend fun clickChapter(chapter: Int) {
        // Region containing all chapters
        val cRegion = region.subRegion(407, 146, 283, 934)
        // Top 1/4 part of lsRegion
        val upperSwipeRegion = cRegion.subRegion(
                cRegion.width / 2 - 15,
                0,
                30,
                cRegion.height / 4
        ) as AndroidRegion
        // Lower 1/4 part of lsRegion
        val lowerSwipeRegion = cRegion.subRegion(
                cRegion.width / 2 - 15,
                cRegion.height / 4 + cRegion.height / 2,
                30,
                cRegion.height / 4
        ) as AndroidRegion
        var retries = 0
        while (cRegion.doesntHave(FileTemplate("chapters/$chapter.png", CHAPTER_SIMILARITY))) {
            navigator.checkLogistics()
            val chapters = (CHAPTER_MIN..CHAPTER_MAX).filterAsync {
                cRegion.has(FileTemplate("chapters/$it.png", CHAPTER_SIMILARITY))
            }
            logger.debug("Visible chapters: $chapters")
            when {
                chapter <= chapters.min() ?: CHAPTER_MAX / 2 -> {
                    logger.debug("Swiping down the chapters")
                    upperSwipeRegion.swipeTo(lowerSwipeRegion)
                }
                chapter >= chapters.max() ?: CHAPTER_MAX / 2 + 1 -> {
                    logger.debug("Swiping up the chapters")
                    lowerSwipeRegion.swipeTo(upperSwipeRegion)
                }
            }
            delay(300)
            if (retries++ >= 3) {
                logger.error("Failed to find and click chapter")
                coroutineContext.cancelAndYield()
            }
        }
        navigator.checkLogistics()
        delay(200)
        cRegion.subRegion(0, 0, 195, cRegion.height).clickTemplateWhile(
                template = FileTemplate("chapters/$chapter.png"),
                timeout = 20
        ) { has(it) }
    }
}