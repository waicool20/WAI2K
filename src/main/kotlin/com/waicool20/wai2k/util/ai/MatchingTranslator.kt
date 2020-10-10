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

import ai.djl.modality.cv.Image
import ai.djl.modality.cv.transform.Resize
import ai.djl.ndarray.NDList
import ai.djl.ndarray.index.NDIndex
import ai.djl.ndarray.types.DataType
import ai.djl.translate.Batchifier
import ai.djl.translate.Pipeline
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import boofcv.struct.geo.AssociatedPair
import com.waicool20.cvauto.util.wrapper.Config
import com.waicool20.cvauto.util.wrapper.KFactoryMultiViewRobust
import georegression.struct.homography.Homography2D_F64
import georegression.struct.point.Point2D_F64

class MatchingTranslator(
    private val resizeWidth: Int = 640,
    private val resizeHeight: Int = 480
) : Translator<Pair<Image, Image>, Homography2D_F64> {
    class ModelMatchingFailedException : Exception()

    private var img0Width = -1
    private var img0Height = -1
    private var img1Width = -1
    private var img1Height = -1

    override fun getBatchifier() = Batchifier.STACK

    override fun getPipeline() = Pipeline(Resize(resizeWidth, resizeHeight), TransposeNormalizeTransform())

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

    override fun processOutput(ctx: TranslatorContext, list: NDList): Homography2D_F64 {
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

        val scales0 = ctx.ndManager.create(floatArrayOf(img0Width.toFloat() / resizeWidth, img0Height.toFloat() / resizeHeight))
        val scales1 = ctx.ndManager.create(floatArrayOf(img1Width.toFloat() / resizeWidth, img1Height.toFloat() / resizeHeight))

        kpts0 = kpts0 * scales0
        kpts1 = kpts1 * scales1

        val points1 = kpts0.toPoint2D().map { Point2D_F64(it.x, it.y) }
        val points2 = kpts1.toPoint2D().map { Point2D_F64(it.x, it.y) }
        val confArr = conf.toFloatArray()

        val pairs = mutableListOf<AssociatedPair>()
        for (i in points1.indices) {
            if (confArr[i] > 0.45) {
                pairs.add(AssociatedPair(points1[i], points2[i], false))
            }
        }

        val modelMatcher = KFactoryMultiViewRobust.homographyRansac(
            Config.Ransac(60, 3.0)
        )

        if (!modelMatcher.process(pairs)) throw ModelMatchingFailedException()
        return modelMatcher.modelParameters.copy()
    }
}