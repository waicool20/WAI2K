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

package com.waicool20.wai2k.script

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.waicool20.cvauto.core.AnyRegion
import com.waicool20.cvauto.core.util.cropIntoRect
import com.waicool20.wai2k.util.loggerFor
import java.awt.Rectangle
import java.lang.IllegalArgumentException

/**
 * Represents and asset and its expected geometry
 *
 * @param path path to asset
 * @param x x coordinate of the asset
 * @param y y coordinate of the asset
 * @param width Width of the asset
 * @param height Height of the asset
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Asset(
    val path: String = "",
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val threshold: Double = 0.89
) {
    private val logger = loggerFor<Asset>()
    /**
     * Gets the region containing this asset
     *
     * @param region Parent region (Should always be full android screen)
     */
    fun getSubRegionFor(region: AnyRegion): AnyRegion {
        val rect = Rectangle(x, y, width, height)
            .apply { grow(3, 3) }
            .cropIntoRect(region)
        return try {
            region.subRegion(rect.x, rect.y, rect.width, rect.height)
        } catch (e: IllegalArgumentException) {
            logger.error("Could not get sub-region for asset: $path")
            throw e
        }
    }
}
