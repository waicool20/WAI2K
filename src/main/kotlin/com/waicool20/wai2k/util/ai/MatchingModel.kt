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
import ai.djl.ndarray.NDList
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.Shape
import ai.djl.nn.AbstractBlock
import ai.djl.training.ParameterStore
import ai.djl.util.PairList
import java.nio.file.Path

class MatchingModel(
    superpointModelPath: Path,
    superglueModelPath: Path
) : Model by Model.newInstance("MatchingModel", "PyTorch") {
    private val superpoint = ModelLoader.loadModel(superpointModelPath)
    private val superglue = ModelLoader.loadModel(superglueModelPath)

    init {
        block = object : AbstractBlock(2) {
            override fun forward(parameterStore: ParameterStore, inputs: NDList, training: Boolean, params: PairList<String, Any>?): NDList {
                val (img0, img1) = inputs
                val pred0 = superpoint.block.forward(parameterStore, NDList(img0), training, params)
                val pred1 = superpoint.block.forward(parameterStore, NDList(img1), training, params)
                val data = NDList().apply {
                    add(img0)
                    addAll(pred0)
                    add(img1)
                    addAll(pred1)
                }
                return NDList().apply {
                    addAll(pred0)
                    addAll(pred1)
                    addAll(superglue.block.forward(parameterStore, data, training, params))
                }
            }

            override fun getOutputShapes(manager: NDManager, inputShapes: Array<out Shape>): Array<Shape> {
                return emptyArray()
            }
        }
    }

    override fun close() {
        superpoint.close()
        superglue.close()
    }
}
