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

import com.waicool20.wai2k.views.tabs.profile.AbstractProfileView
import javafx.scene.control.CheckBox
import javafx.scene.layout.VBox
import tornadofx.*

class StopView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/profile/stop/stop.fxml")
    private val enabledCheckBox: CheckBox by fxid()
    private val exitProgramCheckBox: CheckBox by fxid()
    override fun setValues() {}

    override fun createBindings() {
        profile.stop.apply {
            enabledCheckBox.bind(enabledProperty)
            exitProgramCheckBox.bind(exitProgramProperty)
        }
    }
}
