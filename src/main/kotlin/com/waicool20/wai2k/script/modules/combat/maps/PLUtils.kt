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
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlin.random.Random

object PLUtils {
    private val logger = loggerFor<PLUtils>()

    /**
     *  Enters the chapter based off the EventMapName.kt to then be handed off to its enterMap()
     */
    suspend fun enterChapter(sc: ScriptComponent) {
        delay(3000)
        val region = sc.region
        // Assume name is formatted as "EventPL1_1.." Style
        val targetChapter = sc::class.java.simpleName.drop(7).take(1).toInt()

        // Region on the left where the chapter dossier image will appear when you are in the corresponding chapter
        // Doubles as the back button to return to chapter select
        val r = region.subRegion(380, 500, 60, 60)

        when (val currentChapter = (1..5).firstOrNull { r.has(FileTemplate("combat/maps/EventPL/Ch$it.png", 1.0)) }) {
            null -> logger.info("At chapter selection screen")
            targetChapter -> {
                logger.info("Already at chapter $targetChapter")
                return
            }
            else -> {
                logger.info("At chapter $currentChapter, going back to chapter selection screen")
                // Go back to chapter selection screen
                r.click()
                delay(3000)
            }
        }

        // why can you even zoom on this menu Mica?
        region.pinch(
            Random.nextInt(900, 1000),
            Random.nextInt(300, 400),
            0.0,
            1000)

        // Region for panning up and down
        val r1 = region.subRegionAs<AndroidRegion>(415, 330, 100, 100)
        val r2 = r1.copyAs<AndroidRegion>(y = r1.y + 500)

        logger.info("Pan Down to bottom of chapter select")
        repeat(3) {
            r2.swipeTo(r1)
        }

        // hardcoded approx coordinates of dossier entry
        // Ch1 doesn't pan at all, 2/3 pan once and 4/5 pan twice
        repeat((targetChapter - 1) / 2 ){
            r1.swipeTo(r2)
        }
        when (targetChapter) {
            1 -> region.subRegion(900, 480, 300, 300).click()
            2 -> region.subRegion(1440, 550, 300, 300).click()
            3 -> region.subRegion(320, 350, 300, 300).click()
            4 -> region.subRegion(1200, 315, 300, 300).click()
            5 -> region.subRegion(650, 160, 90, 90).click()
        }
    }
}