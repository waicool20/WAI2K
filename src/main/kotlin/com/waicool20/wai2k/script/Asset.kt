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

package com.waicool20.wai2k.script

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.Region

/**
 * Represents and asset and its expected geometry
 *
 * @param imageName name of the image (without .png extension)
 * @param x x coordinate of the asset
 * @param y y coordinate of the asset
 * @param width Width of the asset
 * @param height Height of the asset
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Asset(
    val imageName: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    /**
     * Path prefix
     */
    var prefix = ""

    /**
     * Image path constructed from [prefix] and [imageName] with .png extension
     */
    val imagePath get() = "$prefix$imageName.png"

    /**
     * Gets the region containing this asset
     *
     * @param region Parent region (Should always be full android screen)
     */
    fun getSubRegionFor(region: Region<AndroidDevice>): Region<AndroidDevice> {
        return region.subRegion(x, y, width, height)
    }
}