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
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlin.random.Random

object SCUtils {
    private val logger = loggerFor<SCUtils>()

    enum class Difficulty {
        NORMAL, HARD
    }

    suspend fun setDifficulty(sc: ScriptComponent, difficulty: Difficulty) {
        val ocrRegion = sc.region.subRegion(192, 983, 86, 84)
        logger.info("Checking difficulty")
        val current = if (Ocr.forConfig(sc.config)
                .doOCRAndTrim(ocrRegion)
                .contains("Hard", ignoreCase = true)) {
            Difficulty.HARD
        } else {
            Difficulty.NORMAL
        }
        if (difficulty != current) {
            logger.info("Changing difficulty")
            ocrRegion.click()
            delay(500)
        }
    }

    suspend fun enterChapter(sc: ScriptComponent) {
        delay(3000)
        val region = sc.region
        val targetChapter = sc::class.java.simpleName.drop(7).take(1).toInt()
        val r = region.subRegion(320, 475, 60, 60)
        when (val currentChapter = (0..5).firstOrNull { r.has(FileTemplate("combat/maps/EventSC/ch$it.png")) }) {
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

        region.pinch(
            Random.nextInt(900, 1000),
            Random.nextInt(300, 400),
            0.0,
            1000)

        val r1 = region.subRegionAs<AndroidRegion>(415, 330, 100, 100)
        val r2 = r1.copyAs<AndroidRegion>(y = r1.y + 500)

        repeat(3) {
            r2.swipeTo(r1)
        }

        if (targetChapter in 1..3) {
            when (targetChapter) {
                1 -> region.subRegion(1126, 478, 90, 90).click()
                2 -> region.subRegion(1567, 281, 90, 90).click()
                3 -> region.subRegion(780, 165, 90, 90).click()
            }
            return
        }

        repeat(3) {
            r1.swipeTo(r2)
        }

        when (targetChapter) {
            4 -> region.subRegion(1354, 986, 90, 90).click()
            5 -> region.subRegion(958, 632, 90, 90).click()
        }
    }
}