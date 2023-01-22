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

import com.waicool20.cvauto.core.template.FT
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.delay


object FPUtils {
    private val logger = loggerFor<FPUtils>()

    suspend fun enterInfinity(sc: ScriptComponent) {
        logger.info("Navigating to chapter infinity")
        val assetPath = "combat/maps/EventFP-Common"

        val arrowR = sc.region.subRegion(32, 421, 48, 80)
        val infR = sc.region.subRegion(317, 605, 60, 60)

        if (infR.doesntHave(FT("$assetPath/inf.png"))) {
            if (arrowR.has(FT("$assetPath/arrow.png"))) {
                logger.info("Not at chapter infinity, going back")
                arrowR.click()
                delay(1000)
            }

            // Swipe up
            val r1 = sc.region.subRegion(900, 900, 500, 100)
            val r2 = r1.copy(y = r1.y - 500)
            r1.swipeTo(r2)

            // Enter infinity
            sc.region.subRegion(1712, 212, 195, 63).click()

            delay(2000)
        }
    }
}
