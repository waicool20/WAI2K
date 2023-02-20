/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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
    baseNavigationDelay: Int = 1000,
    mouseDelay: Double = 0.3,
    defaultSimilarityThreshold: Double = 0.9,
    mapRunnerSimilarityThreshold: Double = 0.85,
    ocrThreshold: Double = 0.9,
    maxPostBattleClick: Int = -1,
    idleAtHome: Boolean = true
) {
    val loopDelayProperty = loopDelay.toProperty()
    val baseNavigationDelayProperty = baseNavigationDelay.toProperty()
    val mouseDelayProperty = mouseDelay.toProperty()
    val defaultSimilarityThresholdProperty = defaultSimilarityThreshold.toProperty()
    val mapRunnerSimilarityThresholdProperty = mapRunnerSimilarityThreshold.toProperty()
    val ocrThresholdProperty = ocrThreshold.toProperty()
    val maxPostBattleClickProperty = maxPostBattleClick.toProperty()
    val idleAtHomeProperty = idleAtHome.toProperty()

    var loopDelay by loopDelayProperty
    var baseNavigationDelay by baseNavigationDelayProperty
    var mouseDelay by mouseDelayProperty
    var defaultSimilarityThreshold by defaultSimilarityThresholdProperty
    var mapRunnerSimilarityThreshold by mapRunnerSimilarityThresholdProperty
    var ocrThreshold by ocrThresholdProperty
    var maxPostBattleClick by maxPostBattleClickProperty
    var idleAtHome by idleAtHomeProperty
}
