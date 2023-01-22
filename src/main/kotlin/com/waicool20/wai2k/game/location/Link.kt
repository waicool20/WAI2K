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

package com.waicool20.wai2k.game.location

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.waicool20.cvauto.core.AnyRegion
import com.waicool20.cvauto.core.template.FT
import com.waicool20.wai2k.script.Asset
import com.waicool20.wai2k.script.BadLinkException
import com.waicool20.wai2k.util.loggerFor
import kotlin.math.roundToInt

/**
 * Represents a link to another game location
 *
 * @param dest Destination of this link
 * @param assets The corresponding asset that should be clicked to get to [dest]
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Link(
    val dest: LocationId, val assets: List<AssetContainer> = emptyList()
) {
    private val logger = loggerFor<Link>()

    class AssetContainer(val condition: String = "-", val asset: Asset)

    val skippable = assets.isEmpty()

    fun clickForRegion(region: AnyRegion) {
        selectAsset(region).getSubRegionFor(region).apply {
            // Shrink region slightly to 90% of defined size
            grow((width * -0.1).roundToInt(), (height * -0.1).roundToInt())
        }.click()
    }

    private fun selectAsset(region: AnyRegion): Asset {
        if (assets.size == 1) return assets.first().asset

        for ((i, ac) in assets.withIndex()) {
            if (ac.condition == "-") {
                logger.debug("Dynamically selected link asset index $i")
                return ac.asset
            }
            val conditionTokens = ac.condition.split("|")

            if (conditionTokens.size != 3) {
                throw BadLinkException(this, "Conditions must contain 3 tokens")
            }

            val (mode, roi, content) = conditionTokens

            val (x, y, width, height) = try {
                roi.split(",").map { it.toInt() }
            } catch (e: NumberFormatException) {
                throw BadLinkException(this, "Condition ROI has some bad coordinates")
            }

            val isSelected = when (mode) {
                "-" -> region.subRegion(x, y, width, height).has(FT(content))
                "!" -> region.subRegion(x, y, width, height).doesntHave(FT(content))
                else -> throw BadLinkException(this, "Condition mode must be - or !")
            }

            if (isSelected) {
                logger.debug("Dynamically selected link asset index $i")
                return ac.asset
            }
        }
        throw BadLinkException(this, "Could not select asset")
    }

    override fun toString() = "Link(dest=$dest)"
}
