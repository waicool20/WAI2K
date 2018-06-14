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

import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.game.LogisticsSupport
import com.waicool20.waicoolutils.controlsfx.bind
import com.waicool20.waicoolutils.javafx.listen
import javafx.beans.property.SimpleListProperty
import javafx.scene.layout.VBox
import org.controlsfx.control.CheckComboBox
import tornadofx.*

@Suppress("UNCHECKED_CAST")
class AssignmentsView : View() {
    override val root: VBox by fxml("/views/tabs/profile/logistics/assignments.fxml")
    private val comboBoxes = (1..10).mapNotNull {
        fxmlLoader.namespace["echelon${it}CCBox"] as? CheckComboBox<Int>
    }

    private val context: Wai2KContext by inject()

    override fun onDock() {
        super.onDock()
        setValues()
        createBindings()
        context.currentProfileProperty.listen { createBindings() }
    }

    fun setValues() {
        comboBoxes.forEach {
            it.items.setAll(LogisticsSupport.list.map { it.number })
        }
    }

    fun createBindings() {
        comboBoxes.forEachIndexed { index, box ->
            context.currentProfile.logistics.assignments.getOrPut(index + 1) {
                SimpleListProperty<Int>()
            }.let { box.bind(it) }
        }
    }
}
