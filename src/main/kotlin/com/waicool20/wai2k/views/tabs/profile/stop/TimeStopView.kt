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

package com.waicool20.wai2k.views.tabs.profile.stop

import com.waicool20.wai2k.config.Wai2KProfile.Stop.Time.Mode
import com.waicool20.wai2k.views.tabs.profile.AbstractProfileView
import com.waicool20.waicoolutils.javafx.addListener
import javafx.scene.Node
import javafx.scene.control.CheckBox
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.VBox
import tornadofx.*

class TimeStopView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/profile/stop/time-stop.fxml")
    private val enabledCheckBox: CheckBox by fxid()
    private val timeStopMode: ToggleGroup by fxid()
    private val elapsedTimeToggle: ToggleButton by fxid()
    private val specificTimeToggle: ToggleButton by fxid()
    private val content: Node by fxid()
    override fun setValues() {}

    override fun createBindings() {
        context.currentProfile.stop.time.apply {
            enabledCheckBox.bind(enabledProperty)
            when (mode) {
                Mode.ELAPSED_TIME -> timeStopMode.selectToggle(elapsedTimeToggle)
                Mode.SPECIFIC_TIME -> timeStopMode.selectToggle(specificTimeToggle)
                else -> Unit // Do nothing
            }
        }
        timeStopMode.selectedToggleProperty().addListener("TimeStopModeToggleListener") { _ -> updateMode() }
        updateMode()
    }

    private fun updateMode() {
        context.currentProfile.stop.time.apply {
            when (timeStopMode.selectedToggle) {
                elapsedTimeToggle -> {
                    content.replaceChildren(find<TimeStopElapsedTimeView>().root)
                    mode = Mode.ELAPSED_TIME
                }
                specificTimeToggle -> {
                    content.replaceChildren(find<TimeStopSpecificTimeView>().root)
                    mode = Mode.SPECIFIC_TIME
                }
                else -> Unit // Do nothing
            }
        }
    }
}