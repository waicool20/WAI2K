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
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay

object CampaignUtils {
    private val logger = loggerFor<CampaignUtils>()

    /**
     *  Arctic Warfare, Operation Cube+, Deep Dive, Singularity, Continuum Turbulence, Isomer, Shattered Connexion
     */
    private val campaigns = listOf("AW", "OC", "DD", "SI", "CT", "IS", "SC")
    private val chapters = listOf(3, 2, 3, 3, 4, 1, 5)

    /**
     *  Selects the campaign on the left scroll menu based off map name
     */
    suspend fun selectCampaign(sc: ScriptComponent) {
        val target = sc::class.simpleName!!
            .removePrefix("Campaign")
            .take(2)
            .let { campaigns.indexOf(it) }

        delay(500)
        logger.info("Selecting campaign: ${campaigns[target]}")
        sc.region.subRegion(395, 154 + target * 153, 193, 133).click()
    }

    /**
     *  Selects the chapter of the campaign event from the top right.
     *  Campaigns with 3D map selection will start the 3d map on the selected chapter
     */
    suspend fun selectCampaignChapter(sc: ScriptComponent) {
        val chaptersMax = sc::class.simpleName!!
            .removePrefix("Campaign")
            .take(2)
            .let { chapters[campaigns.indexOf(it)] }

        val target = sc::class.simpleName!!
            .drop(11)
            .take(1)
            .toInt()

        delay(500)
        logger.info("Selecting chapter: $target")
        sc.region.subRegion(1887 - (chaptersMax - target) * 174, 247, 110, 75).click()
    }

    /**
     *  Enter a simple map in a chapter similar to regular chapters
     *  Limited to the first 3 campaigns
     *  later campaigns can enter thiet sub map selection via map 1
     */
    suspend fun enterSimpleMap(sc: ScriptComponent) {
        val target = sc::class.simpleName!!
            .dropLastWhile { it.isLetter() }
            .takeLast(1)
            .toInt()

        delay(500)
        // Click the map number
        sc.region.subRegion(925, 377 + 177 * (target - 1), 440, 130).click()
        // Click Normal Battle for campaigns in which it appears
        sc.region.subRegion(1445, 830, 345, 135)
            .waitHas(FileTemplate("combat/battle/normal.png"), 3000)?.click()
    }
}