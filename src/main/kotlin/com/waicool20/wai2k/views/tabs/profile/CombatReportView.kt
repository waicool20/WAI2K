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

package com.waicool20.wai2k.views.tabs.profile

import com.waicool20.wai2k.config.Wai2KProfile.CombatReport
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.layout.VBox
import tornadofx.*

class CombatReportView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/profile/combat-report.fxml")
    private val enableCombatReportCheckBox: CheckBox by fxid()
    private val typeComboBox: ComboBox<CombatReport.Type> by fxid()

    override fun setValues() {
        typeComboBox.items.setAll(CombatReport.Type.values().toList())
    }

    override fun createBindings() {
        context.currentProfile.combatReport.apply {
            enableCombatReportCheckBox.bind(enabledProperty)
            typeComboBox.bind(typeProperty)
        }
    }
}