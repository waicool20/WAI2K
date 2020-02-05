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
fun BufferedImage.extractNodes(includeWhite: Boolean = true): GrayF32 {
    val hsv = asPlanar().asHsv()
    val redNodes = hsv.clone().apply { hsvFilter(hueRange = arrayOf(0..10, 350..360), satRange = arrayOf(20..100)) }.getBand(2)
    val blueNodes = hsv.apply { hsvFilter(hueRange = 190..210, satRange = 20..100) }.getBand(2)
    val img = if (includeWhite) {
        val whiteNodes = hsv.clone().apply { hsvFilter(satRange = 0..1, valRange = 210..255) }.getBand(2)
        whiteNodes + redNodes + blueNodes
    } else {
        redNodes + blueNodes
    }

    for (y in 0 until height) {
        var index = img.startIndex + y * img.stride
        for (x in 0 until width) {
            img.data[index] = if (img.data[index] >= 175) 255f else 0f
            index++
        }
    }

    return img
}