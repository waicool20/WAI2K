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

import boofcv.alg.color.ColorLab
import boofcv.struct.image.GrayF32
import com.waicool20.cvauto.util.asHsv
import com.waicool20.cvauto.util.asPlanar
import com.waicool20.cvauto.util.hsvFilter
import com.waicool20.cvauto.util.plus
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * Returns a masked image where nodes and path lines are located
 */
fun BufferedImage.extractNodes(
    includeBlue: Boolean = true,
    includeWhite: Boolean = true,
    includeYellow: Boolean = true
): GrayF32 {
    val hsv = asPlanar().asHsv()
    val redNodes = hsv.clone()
        .apply { hsvFilter(hueRange = arrayOf(0..10, 250..360), satRange = arrayOf(12..100)) }
        .getBand(2)
    var img = redNodes
    if (includeBlue) {
        val blueNodes =
            hsv.clone().apply { hsvFilter(hueRange = 197..210, satRange = 12..100) }.getBand(2)
        img += blueNodes
    }
    if (includeWhite) {
        val whiteNodes =
            hsv.clone().apply { hsvFilter(satRange = 0..1, valRange = 210..255) }.getBand(2)
        img += whiteNodes
    }
    if (includeYellow) {
        val yellowNodes =
            hsv.clone().apply { hsvFilter(hueRange = 40..50, satRange = 12..100) }.getBand(2)
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

/**
 * Compares colors using deltaE formula
 *
 * Rule of thumb:
 *   - 0: Same color
 *   - (0, 1.0]: Basically the same color
 *   - (1.0 to 3.0]: Maybe some slight differences, but still the same color
 *   - (3.0 to 5.0]: Noticeably different
 *   - 5.0+: Different colors
 * @return true if difference is within maxDelta
 */
fun Color.isSimilar(other: Color, maxDelta: Double = 3.0): Boolean {
    val thisLAB = DoubleArray(3)
    val otherLAB = DoubleArray(3)
    ColorLab.rgbToLab(red, green, blue, thisLAB)
    ColorLab.rgbToLab(other.red, other.green, other.blue, otherLAB)
    val deltaE = calculateDeltaE(
        thisLAB[0], thisLAB[1], thisLAB[2],
        otherLAB[0], otherLAB[1], otherLAB[2]
    )
    return deltaE <= maxDelta
}

/**
 * @see [OpenImaj](https://github.com/openimaj/openimaj/blob/df945aea4d037dbfae89ebb475966d1062c1cb63/image/image-processing/src/main/java/org/openimaj/image/analysis/colour/CIEDE2000.java#L85)
 */
private fun calculateDeltaE(
    L1: Double, a1: Double, b1: Double,
    L2: Double, a2: Double, b2: Double
): Double {
    val Lmean = (L1 + L2) / 2.0
    val C1 = sqrt(a1 * a1 + b1 * b1)
    val C2 = sqrt(a2 * a2 + b2 * b2)
    val Cmean = (C1 + C2) / 2.0
    val G = (1 - sqrt(Cmean.pow(7.0) / (Cmean.pow(7.0) + 25.0.pow(7.0)))) / 2
    val a1prime = a1 * (1 + G)
    val a2prime = a2 * (1 + G)
    val C1prime = sqrt(a1prime * a1prime + b1 * b1)
    val C2prime = sqrt(a2prime * a2prime + b2 * b2)
    val Cmeanprime = (C1prime + C2prime) / 2
    val h1prime = atan2(b1, a1prime) + 2 * Math.PI * if (atan2(b1, a1prime) < 0) 1 else 0
    val h2prime = atan2(b2, a2prime) + 2 * Math.PI * if (atan2(b2, a2prime) < 0) 1 else 0
    val Hmeanprime = if (abs(h1prime - h2prime) > Math.PI) {
        (h1prime + h2prime + 2 * Math.PI) / 2
    } else {
        (h1prime + h2prime) / 2
    }
    val T = (1.0
        - 0.17 * cos(Hmeanprime - Math.PI / 6.0) + 0.24 * cos(2 * Hmeanprime)
        + 0.32 * cos(3 * Hmeanprime + Math.PI / 30)
        - 0.2 * cos(4 * Hmeanprime - 21 * Math.PI / 60))
    val deltahprime = when {
        abs(h1prime - h2prime) <= Math.PI -> h2prime - h1prime
        h2prime <= h1prime -> h2prime - h1prime + 2 * Math.PI
        else -> h2prime - h1prime - 2 * Math.PI
    }
    val deltaLprime = L2 - L1
    val deltaCprime = C2prime - C1prime
    val deltaHprime = 2.0 * sqrt(C1prime * C2prime) * sin(deltahprime / 2.0)
    val SL = 1.0 + 0.015 * (Lmean - 50) * (Lmean - 50) / sqrt(20 + (Lmean - 50) * (Lmean - 50))
    val SC = 1.0 + 0.045 * Cmeanprime
    val SH = 1.0 + 0.015 * Cmeanprime * T
    val deltaTheta =
        30 * Math.PI / 180 * exp(-((180 / Math.PI * Hmeanprime - 275) / 25) * ((180 / Math.PI * Hmeanprime - 275) / 25))
    val RC = 2 * sqrt(Cmeanprime.pow(7.0) / (Cmeanprime.pow(7.0) + 25.0.pow(7.0)))
    val RT = -RC * sin(2 * deltaTheta)
    val KL = 1.0
    val KC = 1.0
    val KH = 1.0
    return sqrt(
        deltaLprime / (KL * SL) * (deltaLprime / (KL * SL)) +
            deltaCprime / (KC * SC) * (deltaCprime / (KC * SC)) +
            deltaHprime / (KH * SH) * (deltaHprime / (KH * SH)) +
            RT * (deltaCprime / (KC * SC)) * (deltaHprime / (KH * SH))
    )
}