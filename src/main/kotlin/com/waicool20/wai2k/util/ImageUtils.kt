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

package com.waicool20.wai2k.util

import boofcv.struct.image.GrayF32
import com.waicool20.cvauto.util.asHsv
import com.waicool20.cvauto.util.asPlanar
import com.waicool20.cvauto.util.hsvFilter
import com.waicool20.cvauto.util.plus
import java.awt.image.BufferedImage

/**
 * Returns a masked image where nodes and path lines are located
 */
fun BufferedImage.extractNodes(
    includeBlue: Boolean = true,
    includeWhite: Boolean = true,
    includeYellow: Boolean = true
): GrayF32 {
    val hsv = asPlanar().asHsv()
    val redNodes = hsv.clone().apply { hsvFilter(hueRange = arrayOf(0..10, 250..360), satRange = arrayOf(12..100)) }.getBand(2)
    var img = redNodes
    if (includeBlue) {
        val blueNodes = hsv.clone().apply { hsvFilter(hueRange = 197..210, satRange = 12..100) }.getBand(2)
        img += blueNodes
    }
    if (includeWhite) {
        val whiteNodes = hsv.clone().apply { hsvFilter(satRange = 0..1, valRange = 210..255) }.getBand(2)
        img += whiteNodes
    }
    if (includeYellow) {
        val yellowNodes = hsv.clone().apply { hsvFilter(hueRange = 40..50, satRange = 12..100) }.getBand(2)
        img += yellowNodes
    }

    return img.binarizeImage(0.75)
}

fun GrayF32.binarizeImage(threshold: Double = 0.4): GrayF32 {
    for (y in 0 until height) {
        var index = startIndex + y * stride
        for (x in 0 until width) {
            data[index] = if (data[index] >= 255 * threshold) 255f else 0f
            index++
        }
    }
    return this
}