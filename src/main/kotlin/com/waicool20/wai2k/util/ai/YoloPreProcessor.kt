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

package com.waicool20.wai2k.util.ai

import ai.djl.Model
import ai.djl.modality.cv.util.NDImageUtils
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.types.Shape
import ai.djl.translate.Transform
import kotlin.math.roundToInt

class YoloPreProcessor(model: Model) : Transform {
    private val size = model.getProperty("InputSize")?.toLong()
            ?: error("Model property 'InputSize' must be set")

    override fun transform(array: NDArray): NDArray {
        // Array shape: height, width, layers
        var out = array
        val layers = out.shape[2]
        val width = out.shape[1].toDouble()
        val height = out.shape[0].toDouble()

        when {
            width > height -> {
                val newHeight = (size * (height / width)).roundToInt()
                out = NDImageUtils.resize(array, size.toInt(), newHeight)
                val concatWith = out.manager.zeros(Shape(size - newHeight, size, layers))
                out = out.concat(concatWith, 0)
            }
            height < width -> {
                val newWidth = (size * (width / height)).roundToInt()
                out = NDImageUtils.resize(array, newWidth, size.toInt())
                val concatWith = out.manager.zeros(Shape(size, size - newWidth, layers))
                out = out.concat(concatWith, 1)
            }
            width == height -> {
                out = NDImageUtils.resize(array, size.toInt())
            }
        }
        // Transpose to layers, height, width and normalize
        return out.transpose(2, 0, 1).div(255)
    }
}