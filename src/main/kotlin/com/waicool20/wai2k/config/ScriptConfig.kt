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

package com.waicool20.wai2k.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import tornadofx.*

@JsonIgnoreProperties(ignoreUnknown = true)
class ScriptConfig(
        loopDelay: Int = 15,
        sikulixScanRate: Int = 20,
        defaultSimilarityThreshold: Double = 0.9,
        mapRunnerSimilarityThreshold: Double = 0.85
) {
    val loopDelayProperty = loopDelay.toProperty()
    val sikulixScanRateProperty = sikulixScanRate.toProperty()
    val defaultSimilarityThresholdProperty = defaultSimilarityThreshold.toProperty()
    val mapRunnerSimilarityThresholdProperty = mapRunnerSimilarityThreshold.toProperty()

    var loopDelay by loopDelayProperty
    var sikulixScanRate by sikulixScanRateProperty
    var defaultSimilarityThreshold by defaultSimilarityThresholdProperty
    var mapRunnerSimilarityThreshold by mapRunnerSimilarityThresholdProperty
}