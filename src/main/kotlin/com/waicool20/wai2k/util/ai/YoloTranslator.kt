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
import ai.djl.modality.cv.output.Rectangle
import ai.djl.modality.cv.translator.BaseImageTranslator
import ai.djl.ndarray.NDList
import ai.djl.ndarray.index.NDIndex
import ai.djl.translate.Pipeline
import ai.djl.translate.TranslatorContext
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.superclasses

class YoloTranslator(
        model: Model,
        val threshold: Double,
        val inputImageSize: Pair<Double, Double>,
        val iouThreshold: Double = 0.5
) : BaseImageTranslator<List<GFLObject>>(
        Builder().setPipeline(Pipeline(YoloPreProcessor(model)))
) {
    private class Builder : BaseImageTranslator.BaseBuilder<Builder>() {
        override fun self() = this
    }

    override fun processOutput(ctx: TranslatorContext, list: NDList): List<GFLObject> {
        var output = list[0]
        val inputArraySize = list[1].shape[1] * 32
        val mask = output[NDIndex(":, 4")].gte(threshold).repeat(15).reshape(output.shape)
        output = output.booleanMask(mask)
        output = output.reshape(output.shape[0] / 15, 15)
        val objects = mutableListOf<GFLObject>()
        for (i in 0 until output.shape[0]) {
            // Array format is x1, y1, x2, y2, conf, cls
            val detection = output[i].toFloatArray()
            val (centerX, centerY, w, h, p) = detection
            var x = (centerX - w / 2).toDouble() / inputArraySize
            var y = (centerY - h / 2).toDouble() / inputArraySize
            var width = w.toDouble() / inputArraySize
            var height = h.toDouble() / inputArraySize

            val (imageWidth, imageHeight) = inputImageSize

            when {
                imageWidth > imageHeight -> {
                    val scale = imageWidth / imageHeight
                    y *= scale
                    height *= scale
                }
                imageWidth < imageHeight -> {
                    val scale = imageHeight / imageWidth
                    x *= scale
                    width *= scale
                }
                imageWidth == imageHeight -> Unit // Do Nothing
            }

            x = x.coerceIn(0.0, 1.0)
            y = y.coerceIn(0.0, 1.0)
            width = width.coerceIn(0.0, 1.0)
            height = height.coerceIn(0.0, 1.0)

            val c = detection.slice(5..detection.lastIndex)
            val cMaxIdx = c.indexOf(c.max())
            val obj = try {
                GFLObject.values[cMaxIdx].primaryConstructor?.call(p.toDouble(), Rectangle(x, y, width, height))
            } catch (e: Exception) {
                null
            }
            if (obj != null) objects.add(obj)
        }
        return nms(objects)
    }

    fun nms(boxes: List<GFLObject>): List<GFLObject> {
        val input = boxes.toMutableList()
        val output = mutableListOf<GFLObject>()
        while (input.isNotEmpty()) {
            val best = input.maxBy { it.probability } ?: continue
            input.remove(best)
            input.removeAll { it::class.superclasses == best::class.superclasses && it.bbox.getIoU(best.bbox) >= iouThreshold }
            output.add(best)
        }
        return output
    }
}
