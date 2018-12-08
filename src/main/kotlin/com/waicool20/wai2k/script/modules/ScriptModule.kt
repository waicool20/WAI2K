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
        dollFilterRegions.filter.clickRandomly()
        delay(200)

        if (reset) {
            logger.info("Resetting filters")
            dollFilterRegions.reset.clickRandomly(); yield()
            dollFilterRegions.filter.clickRandomly()
            delay(200)
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