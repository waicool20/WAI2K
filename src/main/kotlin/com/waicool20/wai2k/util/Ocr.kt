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

import com.waicool20.cvauto.core.Region
import com.waicool20.wai2k.config.Wai2KConfig
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage

object Ocr {
    private val numberReplacements = mapOf(
        "cCDGoOQ@" to "0", "iIl\\[\\]|!" to "1",
        "zZ" to "2", "E" to "3",
        "Ah" to "4", "sS" to "5",
        "bG" to "6", "B:" to "8",
        "- —" to ""
    )

    val OCR_DISTANCE_MAP = mapOf(
        "-—ﾗ" to 0.1,
        "0cCDGoOQ@" to 0.1,
        "1iIl\\[\\]|!" to 0.1,
        "2Zz" to 0.1,
        "3E" to 0.2,
        "4Ah" to 0.1,
        "5sS" to 0.1,
        "6Gb" to 0.1,
        "7-" to 0.3,
        "8B:" to 0.2,
        "9S" to 0.3
    )

    const val DIGITS = "0123456789"
    const val ALPHA = "abcdefghijklmnoprstuvwxyz"
    val ALPHA_CAP = ALPHA.toUpperCase()

    fun cleanNumericString(string: String): String {
        var text = string
        numberReplacements.forEach { (r, num) ->
            text = text.replace(Regex("[$r]"), num)
        }
        return text
    }

    /**
     * Returns an instance of [Tesseract] which can be used to do ocr.
     *
     * @param digitsOnly Applies the digit character filter to the engine if true
     * @param useLSTM Uses the new LSTM engine instead of legacy if true
     */
    fun forConfig(
        config: Wai2KConfig,
        digitsOnly: Boolean = false,
        useLSTM: Boolean = false
    ) = Tesseract().apply {
        setTessVariable("user_defined_dpi", "300")
        setDatapath(config.ocrDirectory.toString())
        if (useLSTM) {
            useLSTMEngine()
        } else {
            useLegacyEngine()
        }
        setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK)
        if (digitsOnly) useCharFilter(DIGITS)
    }
}

fun ITesseract.doOCRAndTrim(region: Region<*>) = doOCRAndTrim(region.capture())
fun ITesseract.doOCRAndTrim(image: BufferedImage) = doOCR(image).trim()

fun ITesseract.useCharFilter(chars: String) = apply {
    setTessVariable("tessedit_char_whitelist", chars)
}

fun ITesseract.useLSTMEngine() = apply {
    setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_TESSERACT_LSTM_COMBINED)
}

fun ITesseract.useLegacyEngine() = apply {
    setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY)
}
