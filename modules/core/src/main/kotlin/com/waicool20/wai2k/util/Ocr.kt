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

import com.waicool20.cvauto.core.AnyRegion
import com.waicool20.cvauto.util.pipeline
import com.waicool20.wai2k.config.Wai2kConfig
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.ITesseract
import net.sourceforge.tess4j.Tesseract
import java.awt.Color
import java.awt.image.BufferedImage

object Ocr {
    private val numberReplacements = mapOf(
        "cCDGoOQ@" to "0", "iIl\\[\\]|!" to "1",
        "zZ" to "2", "E" to "3",
        "Ah" to "4", "sS" to "5",
        "bG" to "6", "B:" to "8",
        "—" to "-", " " to ""
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
    val ALPHA_CAP = ALPHA.uppercase()

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
     */
    fun forConfig(config: Wai2kConfig) = Tesseract().apply {
        setVariable("user_defined_dpi", "300")
        setDatapath(config.assetsDirectory.resolve("models").toString())
        blockMode()
    }
}

fun ITesseract.readText(
    region: AnyRegion,
    scale: Double = 1.0,
    threshold: Double = 0.4,
    invert: Boolean = false,
    pad: Int = 20,
    trim: Boolean = true
): String = readText(region.capture(), scale, threshold, invert, pad, trim)

fun ITesseract.readText(
    image: BufferedImage,
    scale: Double = 1.0,
    threshold: Double = 0.4,
    invert: Boolean = false,
    pad: Int = 20,
    trim: Boolean = true
): String {
    val imgp = image.pipeline()
    if (scale > 0.0 && scale != 1.0) imgp.scale(scale)
    if (threshold in 0.0..1.0) imgp.threshold(threshold)
    if (invert) imgp.invert()
    if (pad > 0) imgp.pad(20, 20, 20, 20, Color.WHITE)
    val txt = doOCR(imgp.toBufferedImage())
    return if (trim) txt.trim() else txt
}

fun ITesseract.useCharFilter(chars: String) = apply {
    setVariable("tessedit_char_whitelist", chars)
}

fun ITesseract.digitsOnly() = useCharFilter(Ocr.DIGITS)

fun ITesseract.useLSTMEngine() = apply {
    setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY)
}

fun ITesseract.useLegacyEngine() = apply {
    setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY)
}

fun ITesseract.disableDictionaries() = apply {
    setVariable("load_system_dawg", "false")
    setVariable("load_freq_dawg", "false")
}

fun ITesseract.wordMode() = apply {
    setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_WORD)
}

fun ITesseract.lineMode() = apply {
    setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_LINE)
}

fun ITesseract.blockMode() = apply {
    setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK)
}
