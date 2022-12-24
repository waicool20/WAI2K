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

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

fun BufferedImage.toMat(): Mat {
    val mat = Mat(height, width, CvType.CV_8UC3)
    val data = (raster.dataBuffer as DataBufferByte).data
    mat.put(intArrayOf(0, 0), data)
    return mat
}

fun Mat.toBufferedImage(): BufferedImage {
    val img = BufferedImage(
        width(), height(),
        if (channels() > 3) BufferedImage.TYPE_4BYTE_ABGR else BufferedImage.TYPE_3BYTE_BGR
    )
    val data = (img.raster.dataBuffer as DataBufferByte).data
    get(intArrayOf(0, 0), data)
    return img
}

fun Mat.removeChannels(vararg channelIndex: Int): Mat {
    val dst = mutableListOf<Mat>()
    val tmp = mutableListOf<Mat>()
    Core.split(this, tmp)
    for ((i, mat) in tmp.withIndex()) {
        if (i !in channelIndex) dst.add(mat)
    }
    Core.merge(dst, this)
    return this
}

inline fun <reified T> Mat.at(row: Int, col: Int): Mat.Atable<T> {
    return at(T::class.java, row, col)
}
