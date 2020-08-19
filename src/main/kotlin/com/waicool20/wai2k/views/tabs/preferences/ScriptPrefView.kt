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
import com.waicool20.waicoolutils.javafx.bind
import javafx.scene.control.CheckBox
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.layout.VBox
import tornadofx.*

class ScriptPrefView : View() {
    override val root: VBox by fxml("/views/tabs/preferences/script.fxml")
    private val loopDelaySpinner: Spinner<Int> by fxid()
    private val baseNavigationDelaySpinner: Spinner<Int> by fxid()
    private val mouseDelaySpinner: Spinner<Double> by fxid()
    private val defaultSimThresholdSpinner: Spinner<Double> by fxid()
    private val mapRunnerSimThresholdSpinner: Spinner<Double> by fxid()
    private val fastScreenshotModeCheckBox: CheckBox by fxid()
    private val ocrThresholdSpinner: Spinner<Double> by fxid()
    private val minPostBattleClickSpinner: Spinner<Int> by fxid()
    private val maxPostBattleClickSpinner: Spinner<Int> by fxid()

    private val context: Wai2KContext by inject()

    override fun onDock() {
        super.onDock()
        loopDelaySpinner.valueFactory = IntegerSpinnerValueFactory(0, Int.MAX_VALUE)
        baseNavigationDelaySpinner.valueFactory = IntegerSpinnerValueFactory(0, Int.MAX_VALUE)
        mouseDelaySpinner.valueFactory = DoubleSpinnerValueFactory(0.0, 100.0, 0.15, 0.01)
        defaultSimThresholdSpinner.valueFactory = DoubleSpinnerValueFactory(0.0, 1.0, 0.8, 0.05)
        mapRunnerSimThresholdSpinner.valueFactory = DoubleSpinnerValueFactory(0.0, 1.0, 0.8, 0.05)
        ocrThresholdSpinner.valueFactory = DoubleSpinnerValueFactory(0.0, 1.0, 0.9, 0.01)
        minPostBattleClickSpinner.valueFactory = IntegerSpinnerValueFactory(0, 10)
        maxPostBattleClickSpinner.valueFactory = IntegerSpinnerValueFactory(0, 10)
        context.wai2KConfig.scriptConfig.apply {
            loopDelaySpinner.bind(loopDelayProperty)
            baseNavigationDelaySpinner.bind(baseNavigationDelayProperty)
            mouseDelaySpinner.bind(mouseDelayProperty)
            defaultSimThresholdSpinner.bind(defaultSimilarityThresholdProperty)
            mapRunnerSimThresholdSpinner.bind(mapRunnerSimilarityThresholdProperty)
            ocrThresholdSpinner.bind(ocrThresholdProperty)
            fastScreenshotModeCheckBox.bind(fastScreenshotModeProperty)
            minPostBattleClickSpinner.bind(minPostBattleClickProperty)
            maxPostBattleClickSpinner.bind(maxPostBattleClickProperty)
        }
    }
}