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
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.modality.cv.output.Rectangle
import ai.djl.modality.cv.translator.ObjectDetectionTranslator
import ai.djl.ndarray.NDList
import ai.djl.ndarray.index.NDIndex
import ai.djl.translate.Pipeline
import ai.djl.translate.TranslatorContext

class YoloTranslator(
        model: Model,
        threshold: Double,
        inputImageSize: Pair<Double, Double>,
        val iouThreshold: Double = 0.5
) : ObjectDetectionTranslator(
        Builder()
                .setPipeline(Pipeline(YoloPreProcessor(model)))
                .optThreshold(threshold.toFloat())
                .optSynset(model.getClasses() ?: error("Model property 'Classes' must be set"))
                .optRescaleSize(inputImageSize.first, inputImageSize.second)
) {
    companion object {
        private fun Model.getClasses(): List<String>? {
            return getProperty("Classes")?.split(",")?.map { it.trim() }
        }
    }
    private class Builder : ObjectDetectionTranslator.BaseBuilder<Builder>() {
        override fun self() = this
    }

    override fun processOutput(ctx: TranslatorContext, list: NDList): DetectedObjects {
        var output = list[0]
        val inputArraySize = list[1].shape[1] * 32
        val mask = output[NDIndex(":, 4")].gte(threshold).repeat(15).reshape(output.shape)
        output = output.booleanMask(mask)
        output = output.reshape(output.shape[0] / 15, 15)
        val objects = mutableListOf<DetectedObjects.DetectedObject>()
        for (i in 0 until output.shape[0]) {
            // Array format is x1, y1, x2, y2, conf, cls
            val detection = output[i].toFloatArray()
            val (centerX, centerY, w, h, p) = detection
            var x = (centerX - w / 2).toDouble() / inputArraySize
            var y = (centerY - h / 2).toDouble() / inputArraySize
            var width = w.toDouble() / inputArraySize
            var height = h.toDouble() / inputArraySize

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

            val c = detection.slice(5..detection.lastIndex)
            val cMaxIdx = c.indexOf(c.max())
            objects.add(DetectedObjects.DetectedObject(classes[cMaxIdx], p.toDouble(), Rectangle(x, y, width, height)))
        }
        val keep = nms(objects)
        return DetectedObjects(
                keep.map { it.className },
                keep.map { it.probability },
                keep.map { it.boundingBox }
        )
    }

    fun nms(boxes: List<DetectedObjects.DetectedObject>): List<DetectedObjects.DetectedObject> {
        val input = boxes.toMutableList()
        val output = mutableListOf<DetectedObjects.DetectedObject>()
        while (input.isNotEmpty()) {
            val best = input.maxBy { it.probability } ?: continue
            input.remove(best)
            input.removeAll { it.className == best.className && it.boundingBox.getIoU(best.boundingBox) >= iouThreshold }
            output.add(best)
        }
        return output
    }
}
