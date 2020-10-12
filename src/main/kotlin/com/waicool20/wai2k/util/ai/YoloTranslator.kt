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
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.output.Rectangle
import ai.djl.modality.cv.translator.BaseImageTranslator
import ai.djl.ndarray.NDList
import ai.djl.ndarray.index.NDIndex
import ai.djl.translate.Pipeline
import ai.djl.translate.TranslatorContext
import com.waicool20.waicoolutils.createCompatibleCopy
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.roundToInt
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.primaryConstructor

class YoloTranslator(
    model: Model,
    val threshold: Double,
    val iouThreshold: Double = 0.4
) : BaseImageTranslator<List<GFLObject>>(
    Builder().setPipeline(Pipeline(TransposeNormalizeTransform()))
) {
    private val size = model.getProperty("InputSize")?.toInt()
        ?: error("Model property 'InputSize' must be set")

    private class Builder : BaseImageTranslator.BaseBuilder<Builder>() {
        override fun self() = this
    }

    private var imageWidth = -1.0
    private var imageHeight = -1.0

    override fun processInput(ctx: TranslatorContext, input: Image): NDList {
        imageWidth = input.width.toDouble()
        imageHeight = input.height.toDouble()

        val inputImage = input.wrappedImage as BufferedImage
        val networkInput = inputImage.createCompatibleCopy(size, size)
        val g = (networkInput.graphics as Graphics2D).apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            paint = Color.BLACK
            fillRect(0, 0, size, size)
        }

        if (input.width < size && input.height < size) {
            g.drawImage(inputImage, 0, 0, null)
        } else {
            val width = input.width.toDouble()
            val height = input.height.toDouble()
            when {
                width > height -> {
                    val newHeight = (size * (height / width)).roundToInt()
                    g.drawImage(inputImage, 0, 0, size, newHeight, null)
                }
                height < width -> {
                    val newWidth = (size * (width / height)).roundToInt()
                    g.drawImage(inputImage, 0, 0, newWidth, size, null)
                }
                width == height -> {
                    g.drawImage(inputImage, 0, 0, size, size, null)
                }
            }
        }
        g.dispose()
        return super.processInput(ctx, ImageFactory.getInstance().fromImage(networkInput))
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

            if (imageWidth < size && imageHeight < size) {
                x *= size / imageWidth
                y *= size / imageHeight
                width *= size / imageWidth
                height *= size / imageHeight
            } else {
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
            }

            x = x.coerceIn(0.0, 1.0)
            y = y.coerceIn(0.0, 1.0)
            width = width.coerceIn(0.0, 1.0)
            height = height.coerceIn(0.0, 1.0)

            val c = detection.slice(5..detection.lastIndex)
            val cMaxIdx = c.indexOf(c.maxOrNull())
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
            val best = input.maxByOrNull { it.probability } ?: continue
            input.remove(best)
            input.removeAll {
                (it::class == best::class ||
                    it::class.isSubclassOf(best::class) ||
                    it::class.isSuperclassOf(best::class)) &&
                    it.bbox.getIoU(best.bbox) >= iouThreshold
            }
            output.add(best)
        }
        return output
    }
}
