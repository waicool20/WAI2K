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

import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import com.waicool20.cvauto.core.Region
import java.awt.image.BufferedImage

fun <T> Predictor<Image, T>.predict(image: BufferedImage): T {
    return predict(ImageFactory.getInstance().fromImage(image))
}

fun <T> Predictor<Image, T>.predict(region: Region<*>): T {
    return predict(ImageFactory.getInstance().fromImage(region.capture()))
}