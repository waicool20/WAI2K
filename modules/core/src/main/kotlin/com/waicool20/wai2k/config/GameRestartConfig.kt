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
class GameRestartConfig(
    enabled: Boolean = false,
    averageDelay: Long = 1600,
    delayCoefficientThreshold: Double = 1.5,
    maxRestarts: Int = 15
) {
    val enabledProperty = enabled.toProperty()
    val averageDelayProperty = averageDelay.toProperty()
    val delayCoefficientThresholdProperty = delayCoefficientThreshold.toProperty()
    val maxRestartsProperty = maxRestarts.toProperty()

    var enabled by enabledProperty
    var averageDelay by averageDelayProperty
    val delayCoefficientThreshold by delayCoefficientThresholdProperty
    val maxRestarts by maxRestartsProperty
}
