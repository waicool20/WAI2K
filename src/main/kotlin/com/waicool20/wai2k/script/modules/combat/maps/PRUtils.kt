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

import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.ScriptException
import com.waicool20.wai2k.util.readText
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay


object PRUtils {
    private val logger = loggerFor<PRUtils>()

    enum class Difficulty {
        NORMAL, HARD
    }

    private suspend fun setDifficulty(sc: ScriptComponent, difficulty: Difficulty) {
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

    suspend fun enterInfinity(sc: ScriptComponent, difficulty: Difficulty) {
        setDifficulty(sc, difficulty)

        delay(3000)
        logger.info("Navigating to chapter infinity")

        val arrowR = sc.region.subRegion(32, 421, 48, 79)
        val infR = sc.region.subRegion(275, 523, 41, 41)
        val assetPath = "combat/maps/EventPR-Common"

        if (infR.doesntHave(FileTemplate("$assetPath/inf.png"))) {
            if (arrowR.has(FileTemplate("$assetPath/arrow.png"))) {
                logger.info("Not at chapter infinity, going back")
                arrowR.click()
                delay(1000)
            }

            sc.region.findBest(FileTemplate("$assetPath/inf-ch.png"))?.region
                ?.subRegion(98, 0, 90, 44)?.click()
                ?: throw ScriptException("Couldn't click chapter infinity")

            delay(2000)
        }

        logger.info("At chapter infinity")
        val r1 = sc.region.subRegion(660, 251, 60, 60)
        val r2 = r1.copy(x = r1.x + 200)

        r1.swipeTo(r2)
    }
}
