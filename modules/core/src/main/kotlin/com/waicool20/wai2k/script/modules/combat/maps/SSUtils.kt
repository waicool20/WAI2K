/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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
import com.waicool20.cvauto.core.util.isSimilar
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.wai2k.util.readText
import kotlinx.coroutines.delay
import java.awt.Color


object SSUtils {
    private val logger = loggerFor<SSUtils>()

    enum class Difficulty {
        NORMAL, HARD, Nightmare
    }

    suspend fun setDifficulty(sc: ScriptComponent, difficulty: Difficulty) {
        logger.info("Checking difficulty...")
        val difficultyRegion = sc.region.subRegion(233, 923, 88, 70)
        val current: Difficulty = if (
            sc.ocr.readText((difficultyRegion), threshold = 0.4, pad = 0, trim = false)
                .contains("Normal", true)
        ) {
            Difficulty.NORMAL
        } else {
            if (
                sc.ocr.readText((difficultyRegion), threshold = 0.4, pad = 0, trim = false)
                    .contains("Hard", true)
            ) {
                Difficulty.HARD
            } else {
                Difficulty.Nightmare
            }
        }
        logger.info("Current difficulty: $current")
        if (current != difficulty) {
            logger.info("Changing to $difficulty")
            val clicks = (difficulty.ordinal - current.ordinal + 3) % 3
            repeat(clicks) {
                difficultyRegion.click()
                delay(500)
            }
        }
    }

    suspend fun navToAreaWithMaps(sc: ScriptComponent) {
        if (sc.region.pickColor(266, 860).isSimilar(Color(42, 149, 255))) {
            logger.info("At bottom of tower")
        } else {
            sc.region.subRegion(245, 721, 42, 168).click() // Cpt.3
            logger.info("Moving to bottom of tower...")
            delay(2000)
        }

        val r1 = sc.region.subRegion(1850, 975, 50, 50)
        val r2 = r1.copy(y = r1.y - 700)
        logger.info("Pan down x5")
        repeat(6) {
            r1.swipeTo(r2, 300)
            delay(300)
        }
        logger.info("Pan up x1")
        r2.swipeTo(r2.copy(y = r2.y + 400))
        delay(50)
        r2.click() // Click to stop inertia
    }

    fun findMapDiamond(sc: ScriptComponent): AndroidRegion? {

        val targetDiamond = sc::class.java.simpleName
            .replace("EventSS_", "").take(1).toInt()

        val pin = if (targetDiamond == 1) {
            sc.region.subRegion(0, 0, 960, 1080).findBest(
                FileTemplate(
                    "combat/slow-shock-diamond.png"
                )
            )?.region
        } else {
            sc.region.subRegion(960, 0, 960, 1080).findBest(
                FileTemplate(
                    "combat/slow-shock-diamond.png"
                )
            )?.region
        }
        return pin
    }

}

