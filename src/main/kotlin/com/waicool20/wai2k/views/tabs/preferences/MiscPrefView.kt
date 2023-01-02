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

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.views.tabs.profile.AbstractProfileView
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.layout.VBox
import tornadofx.*

class MiscPrefView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/preferences/misc.fxml")
    private val debugModeEnabledCheckBox: CheckBox by fxid()
    private val captureMethodComboBox: ComboBox<AndroidDevice.CaptureMethod> by fxid()
    private val captureCompressionModeComboBox: ComboBox<AndroidDevice.CompressionMode> by fxid()

    override fun setValues() {
        captureMethodComboBox.items.setAll(AndroidDevice.CaptureMethod.values().toList())
        captureCompressionModeComboBox.items.setAll(AndroidDevice.CompressionMode.values().toList())
    }

    override fun createBindings() {
        Wai2k.config.apply {
            captureMethodComboBox.bind(captureMethodProperty)
            captureCompressionModeComboBox.bind(captureCompressionModeProperty)
        }
    }
}
