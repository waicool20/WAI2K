/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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

import ai.djl.modality.cv.Image
import ai.djl.modality.cv.transform.Resize
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDList
import ai.djl.ndarray.index.NDIndex
import ai.djl.ndarray.types.DataType
import ai.djl.translate.Batchifier
import ai.djl.translate.Pipeline
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f

class MatchingTranslator(
    private val resizeWidth: Int = 640,
    private val resizeHeight: Int = 480
) : Translator<Pair<Image, Image>, Mat> {
    class ModelMatchingFailedException : Exception()

    private var img0Width = -1
    private var img0Height = -1
    private var img1Width = -1
    private var img1Height = -1

    private val pipeline = Pipeline(
        Resize(resizeWidth, resizeHeight),
        TransposeNormalizeTransform()
    )

    val confidenceThreshold = 0.45
    override fun getBatchifier() = Batchifier.STACK

    override fun processInput(ctx: TranslatorContext, input: Pair<Image, Image>): NDList {
        val (img0, img1) = input
        img0Width = img0.width
        img0Height = img0.height
        img1Width = img1.width
        img1Height = img1.height

        val img0array = img0.toNDArray(ctx.ndManager, Image.Flag.GRAYSCALE)
        val img1array = img1.toNDArray(ctx.ndManager, Image.Flag.GRAYSCALE)
        return NDList().apply {
            addAll(pipeline.transform(NDList(img0array)))
            addAll(pipeline.transform(NDList(img1array)))
        }
    }

    override fun processOutput(ctx: TranslatorContext, list: NDList): Mat {
        var kpts0 = list[0]
        val kpts1Temp = list[3]
        var matches = list[6]
        var conf = list[8]

        val keepMask = matches.gt(-1)

        matches = matches.booleanMask(keepMask)
        conf = conf.booleanMask(keepMask)

        kpts0 = kpts0.booleanMask(keepMask.stack(keepMask, 1), 1)
        kpts0 = kpts0.reshape(kpts0.shape[0] / 2, 2)

        var kpts1 = kpts0.zerosLike()

        when (matches.dataType) {
            DataType.INT32 -> {
                for ((i, j) in matches.toIntArray().withIndex()) {
                    kpts1.set(NDIndex(i.toLong()), kpts1Temp.get(j.toLong()))
                }
            }
            DataType.INT64 -> {
                for ((i, j) in matches.toLongArray().withIndex()) {
                    kpts1.set(NDIndex(i.toLong()), kpts1Temp.get(j))
                }
            }
            else -> error("Matches is not integer array!")
        }

        // Currently the values are between 0 and 1, in this format: [[x1, y1], [x2, y2] ...]
        // So we multiply it by [resizeWidth, resizeHeight] to get the final coordinates
        // eg. [[x1 * resizeWidth, y1 * resizeHeight], [x2 * resizeWidth, y2 * resizeHeight] ...]
        val scales0 = ctx.ndManager.create(
            floatArrayOf(img0Width.toFloat() / resizeWidth, img0Height.toFloat() / resizeHeight)
        )
        val scales1 = ctx.ndManager.create(
            floatArrayOf(img1Width.toFloat() / resizeWidth, img1Height.toFloat() / resizeHeight)
        )

        kpts0 = kpts0 * scales0
        kpts1 = kpts1 * scales1

        // Filter out key points that don't meet the confidence threshold
        val confMask = conf.gte(confidenceThreshold).repeat(2).reshape(kpts0.shape)
        kpts0 = kpts0.booleanMask(confMask)
        kpts1 = kpts1.booleanMask(confMask)

        // at least 4 points are required for homography to be calculated
        if (kpts0.size() < 4 || kpts1.size() < 4) throw ModelMatchingFailedException()

        // Do homography
        val h = Calib3d.findHomography(
            kpts0.kptsToMat(),
            kpts1.kptsToMat(),
            Calib3d.USAC_MAGSAC,
            2.0 // This value isn't important for MAGSAC
        )
        if (h.empty()) throw ModelMatchingFailedException()
        return h
    }

    private fun NDArray.kptsToMat(): MatOfPoint2f {
        val array = toFloatArray()
        val mat = Mat(array.size / 2, 1, CvType.CV_32FC2)
        mat.put(0, 0, array)
        return MatOfPoint2f(mat)
    }
}
