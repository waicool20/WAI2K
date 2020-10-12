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

package com.waicool20.wai2k.util.cpd

import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.Shape
import com.waicool20.wai2k.util.ai.*
import kotlin.math.PI
import kotlin.math.pow

/**
 * Coherent Point Drift Algorithm, this estimates and registers a source point set to a target
 * point set. This is the base class where all transformation types inherit from.
 *
 * Magical code ported from: https://github.com/siavashk/pycpd/blob/master/pycpd/emregistration.py
 */
abstract class CPDRegistration(
    val target: NDArray,
    val source: NDArray,
    initSigma2: Double? = null,
    val tolerance: Double = 0.001,
    private val w: Double = 0.0
) : Iterator<CPDRegistration>, Sequence<CPDRegistration> {
    protected val manager = NDManager.newBaseManager()

    init {
        require(target.shape.dimension() == 2) { "Target point cloud must be 2D NDArray" }
        require(source.shape.dimension() == 2) { "Source point cloud must be 2D NDArray" }
        if (initSigma2 != null) {
            require(initSigma2 > 0) { "Expected positive sigma2 value: ${this.sigma2}" }
        }
        require(tolerance > 0) { "Tolerance must be larger than 0: $tolerance" }
        require(w in 0.0..1.0) { "w must be within 0 until 1: $w" }
    }

    protected var sigma2 = initSigma2 ?: run {
        val (N, D) = target.shape
        val (M, _) = source.shape
        val diff = target.expandDims(0) - source.expandDims(1)
        val err = diff.square()
        (err.sum() / (D * M * N)).getDouble()
    }

    var TY = source
        protected set
    var error = Double.POSITIVE_INFINITY
        protected set

    protected val N = target.shape[0]
    protected val D = target.shape[1]
    protected val M = source.shape[0]

    protected var diff = Double.POSITIVE_INFINITY
    protected var P = manager.zeros(Shape(M, N))
    protected var Pt1 = manager.zeros(Shape(N))
    protected var P1 = manager.zeros(Shape(M))
    protected var Np = 0.0

    protected abstract fun updateTransform()
    protected abstract fun transformPointCloud()
    protected abstract fun updateVariance()

    override fun iterator(): Iterator<CPDRegistration> {
        return this
    }

    override fun hasNext() = true

    override fun next(): CPDRegistration {
        expectation()
        maximization()
        return this
    }

    fun getTranslatedPoints() = TY.toPoint2D()

    private fun expectation() {
        P = (target.expandDims(0) - TY.expandDims(1)).square().sum(2)

        val c = run {
            var c = (2 * PI * sigma2).pow(D / 2.0)
            c = c * w / (1 - w)
            c * M / N
        }

        P = (-P / (2 * sigma2)).exp()
        val den = P.sum(0)._stack(M) + Double.MIN_VALUE + c

        P = P / den
        Pt1 = P.sum(0)
        P1 = P.sum(1)
        Np = P1.sum().getDouble()
    }

    private fun maximization() {
        updateTransform()
        transformPointCloud()
        updateVariance()
    }

    protected fun solve(a: NDArray, b: NDArray): NDArray {
        val A = a.toDoubleArray()
        val detA = A[0] * A[3] - A[1] * A[2]
        val temp = A[0]
        A[0] = A[3]
        A[1] = -A[1]
        A[2] = -A[2]
        A[3] = temp
        a.set(A)
        val invA = manager.create(A, a.shape) / detA
        return invA.dot(b)
    }
}