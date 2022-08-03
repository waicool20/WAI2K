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

import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.util.digitsOnly
import com.waicool20.wai2k.util.readText
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlin.random.Random


object MSUtils {
    private val logger = loggerFor<MSUtils>()

    enum class Difficulty {
        NORMAL, HARD
    }

    suspend fun setDifficulty(sc: ScriptComponent, difficulty: Difficulty) {
        val ocrRegion = sc.region.subRegion(230, 925, 79, 24)
        logger.info("Checking difficulty")
        val current = if (
            sc.ocr.readText((ocrRegion), threshold = 0.4, pad = 0, trim = false)
                .contains("Hard", true)
        ) {
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
        val targetChapter = sc::class.java.simpleName.replace("EventMS_", "").take(1).toInt()
        val r = region.subRegion(29, 398, 130, 63)
        when (val currentChapter = sc.ocr.digitsOnly().readText(r, threshold = 0.8).toIntOrNull()) {
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
            1000
        )

        val r1 = sc.region.subRegion(470, 875, 115, 115)
        val r2 = r1.copy(x = r1.x + 100, y = r1.y - 500)

        logger.info("Go to bottom left corner")
        repeat(3) {
            r1.swipeTo(r2)
        }

        if (targetChapter in 1..3) {
            when (targetChapter) {
                1 -> region.subRegion(1101, 432, 322, 417).click()
                2 -> region.subRegion(1519, 213, 322, 191).click()
                3 -> region.subRegion(795, 153, 322, 58).click()
            }
            return
        }

        logger.info("Go to upper right corner")
        repeat(3) {
            r2.swipeTo(r1)
        }

        if (targetChapter == 4) region.subRegion(1126, 251, 322, 417).click()
    }

    suspend fun panRight(sc: ScriptComponent) {
        logger.info("Panning right through map list")
        val r = sc.region.subRegion(2000, 250, 50, 50)
        repeat(2) {
            r.swipeTo(r.copy(x = r.x - 1400), duration = 500)
            delay(300)
        }
    }
}
