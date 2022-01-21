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

package com.waicool20.wai2k.views.tabs.profile.logistics

import com.waicool20.wai2k.game.GFL
import com.waicool20.wai2k.game.LogisticsSupport
import com.waicool20.wai2k.views.tabs.profile.AbstractProfileView
import com.waicool20.waicoolutils.controlsfx.bind
import javafx.beans.property.SimpleListProperty
import javafx.geometry.Insets
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import org.controlsfx.control.CheckComboBox
import tornadofx.*

@Suppress("UNCHECKED_CAST")
class AssignmentsView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/profile/logistics/assignments.fxml")
    private val comboBoxes = mutableListOf<CheckComboBox<Int>>()

    init {
        for (i in 1..GFL.MAX_ECHELON) {
            hbox {
                label("Echelon $i") {
                    prefWidth = 100.0
                }
                val box = CheckComboBox<Int>().apply {
                    converter = object : StringConverter<Int>() {
                        override fun toString(i: Int) = LogisticsSupport.list[i - 1].formattedString
                        override fun fromString(s: String): Int? = null
                    }
                    items.setAll(LogisticsSupport.list.map { it.number })
                    prefWidth = 200.0
                }
                add(box)
                comboBoxes.add(box)
                padding = Insets(3.0, 10.0, 10.0, 3.0)
            }
        }
    }

    override fun setValues() = Unit

    override fun createBindings() {
        comboBoxes.forEachIndexed { index, box ->
            context.currentProfile.logistics.assignments.getOrPut(index + 1) {
                SimpleListProperty()
            }.let { box.bind(it) }
        }
    }
}
