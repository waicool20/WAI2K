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

import ai.djl.modality.cv.output.Point
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.DataType

operator fun NDArray.plus(array: NDArray): NDArray = add(array)
operator fun NDArray.plus(number: Number): NDArray = add(number)
operator fun NDArray.minus(array: NDArray): NDArray = sub(array)
operator fun NDArray.minus(number: Number): NDArray = sub(number)
operator fun NDArray.times(array: NDArray): NDArray = mul(array)
operator fun NDArray.times(number: Number): NDArray = mul(number)

operator fun NDArray.unaryMinus(): NDArray = neg()

operator fun Number.plus(array: NDArray): NDArray = array + this
operator fun Number.minus(array: NDArray): NDArray = array.neg() + this
operator fun Number.times(array: NDArray): NDArray = array * this
operator fun Number.div(array: NDArray): NDArray = array.pow(-1) * this

operator fun NDArray.plusAssign(array: NDArray) {
    addi(array)
}

operator fun NDArray.plusAssign(number: Number) {
    addi(number)
}

operator fun NDArray.minusAssign(array: NDArray) {
    subi(array)
}

operator fun NDArray.minusAssign(number: Number) {
    subi(number)
}

operator fun NDArray.timesAssign(array: NDArray) {
    muli(array)
}

operator fun NDArray.timesAssign(number: Number) {
    muli(number)
}

operator fun NDArray.divAssign(array: NDArray) {
    divi(array)
}

operator fun NDArray.divAssign(number: Number) {
    divi(number)
}

fun NDArray.sum(vararg axes: Int) = sum(axes)

fun NDArray.diag() = manager.eye(shape[0].toInt()) * tile(shape[0]).reshape(shape[0], shape[0])

fun NDArray._trace() = (manager.eye(shape.dimension()) * this).sum()
fun NDArray._stack(repeats: Long) = tile(repeats).reshape(repeats, shape[0])

fun NDArray.toPoint2D(): List<Point> {
    return when (dataType) {
        DataType.FLOAT32 -> {
            val array = toFloatArray()
            List((size() / 2).toInt()) { i -> Point(array[i * 2].toDouble(), array[i * 2 + 1].toDouble()) }
        }
        DataType.FLOAT64 -> {
            val array = toDoubleArray()
            List((size() / 2).toInt()) { i -> Point(array[i * 2], array[i * 2 + 1]) }
        }
        else -> error("Unsupported data type: $dataType")
    }
}

fun List<Point>.toNDArray(): NDArray {
    val array = DoubleArray(size * 2)
    for (i in indices) {
        array[i * 2] = get(i).x
        array[i * 2 + 1] = get(i).y
    }
    return NDManager.newBaseManager().create(array).reshape(size.toLong(), 2)
}