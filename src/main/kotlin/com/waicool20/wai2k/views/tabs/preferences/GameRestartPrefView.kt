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

package com.waicool20.wai2k.views.tabs.preferences

import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.waicoolutils.javafx.LongSpinnerValueFactory
import com.waicool20.waicoolutils.javafx.bind
import javafx.scene.control.CheckBox
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.layout.VBox
import tornadofx.*

class GameRestartPrefView : View() {
    override val root: VBox by fxml("/views/tabs/preferences/game-restart.fxml")
    private val enabledCheckBox: CheckBox by fxid()
    private val averageDelaySpinner: Spinner<Long> by fxid()
    private val delayCoefficientThresholdSpinner: Spinner<Double> by fxid()
    private val maxRestartsSpinner: Spinner<Int> by fxid()

    private val context: Wai2KContext by inject()

    override fun onDock() {
        super.onDock()
        averageDelaySpinner.valueFactory = LongSpinnerValueFactory(0, 9999)
        delayCoefficientThresholdSpinner.valueFactory = DoubleSpinnerValueFactory(1.0, 10.0, 1.0, 0.1)
        maxRestartsSpinner.valueFactory = IntegerSpinnerValueFactory(1, Int.MAX_VALUE)
        with(context.wai2KConfig.gameRestartConfig) {
            enabledCheckBox.bind(enabledProperty)
            averageDelaySpinner.bind(averageDelayProperty)
            delayCoefficientThresholdSpinner.bind(delayCoefficientThresholdProperty)
            maxRestartsSpinner.bind(maxRestartsProperty)
        }
    }
}