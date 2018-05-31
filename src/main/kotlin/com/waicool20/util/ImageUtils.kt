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

package com.waicool20.util

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * Scales a given [BufferedImage] to a given scaling factor, rounded to the nearest integer pixel
 * size.
 *
 * @param scaleFactor Scaling factor
 * @return New scaled [BufferedImage]
 */
fun BufferedImage.scale(scaleFactor: Double = 2.0): BufferedImage {
    val w = (width * scaleFactor).roundToInt()
    val h = (height * scaleFactor).roundToInt()
    val image = BufferedImage(w, h, type)
    (image.graphics as Graphics2D).apply {
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        drawImage(this@scale, 0, 0, w, h, null)
        dispose()
    }
    return image
}

/**
 * Converts a given [BufferedImage] into black and white based on a threshold on each pixels
 * relative luminance
 *
 * @param threshold Deciding threshold, luminance above this will be white else black
 * @return Original [BufferedImage] but in B&W
 */
fun BufferedImage.binarizeImage(threshold: Double = 0.4) = apply {
    for (x in 0 until width) {
        for (y in 0 until height) {
            val newColor = if (Color(getRGB(x, y), true).relativeLuminance() > threshold) {
                Color.WHITE
            } else {
                Color.BLACK
            }
            setRGB(x, y, newColor.rgb)
        }
    }
}

/**
 * Gets the relative luminance of a color
 *
 * @return Relative luminance
 */
fun Color.relativeLuminance() = (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255
