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
import ai.djl.ndarray.types.Shape
import com.waicool20.wai2k.util.ai.*
import kotlin.math.abs
import kotlin.math.ln

/**
 * Coherent Point Drift Algorithm, this estimates and registers a source point set to a target
 * point set. This class provides affine transformation registration.
 *
 * Magical code ported from: https://github.com/siavashk/pycpd/blob/master/pycpd/affine_registration.py
 */
class AffineCPDRegistration(
    X: NDArray,
    Y: NDArray,
    initB: NDArray? = null,
    initT: NDArray? = null
) : CPDRegistration(X, Y) {
    private var B = initB ?: manager.eye(D.toInt())
    private var t = initT ?: manager.zeros(Shape(1, D))
    private lateinit var X_hat: NDArray
    private lateinit var Y_hat: NDArray
    private lateinit var A: NDArray
    private lateinit var YPY: NDArray

    override fun updateTransform() {
        val muX = P.dot(target).sum(0) / Np
        val muY = P.transpose().dot(source).sum(0) / Np

        X_hat = target - muX._stack(N)
        Y_hat = source - muY._stack(M)
        A = X_hat.transpose().dot(P.transpose()).dot(Y_hat)
        YPY = Y_hat.transpose().dot(P1.diag()).dot(Y_hat)
        B = solve(YPY.transpose(), A.transpose())
        t = muX.transpose() - (B.transpose().matMul(muY.transpose()))
    }

    override fun transformPointCloud() {
        TY = source.dot(B) + t.tile(M).reshape(M, D)
    }

    override fun updateVariance() {
        val qprev = error
        // Cant use .trace(), not implemented in PtNDArray
        val trAB = A.dot(B)._trace().getDouble()
        val xPx = Pt1.transpose().dot((X_hat * X_hat).sum(1)).getDouble()
        val trBYPYP = B.dot(YPY).dot(B)._trace().getDouble()
        error = (xPx - 2 * trAB + trBYPYP) / (2 * sigma2) + D * Np / 2 * ln(sigma2)
        diff = abs(error - qprev)
        sigma2 = (xPx - trAB) / (Np * D)

        if (sigma2 <= 0) {
            sigma2 = tolerance / 10
        }
    }
}