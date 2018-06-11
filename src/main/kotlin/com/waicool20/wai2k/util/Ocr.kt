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

package com.waicool20.wai2k.util

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract

object Ocr {
    fun forConfig(config: Wai2KConfig, digitsOnly: Boolean = false) = Tesseract().apply {
        setDatapath(config.ocrDirectory.toString())
        setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK)
        if (digitsOnly) setTessVariable("tessedit_char_whitelist", "0123456789")
    }
}

fun ITesseract.doOCR(region: AndroidRegion) = doOCR(region.takeScreenshot()).trim()

