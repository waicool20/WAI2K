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

package com.waicool20.wai2k.views.tabs.profile.combat

import com.waicool20.wai2k.game.TDoll
import com.waicool20.wai2k.views.tabs.profile.AbstractProfileView
import com.waicool20.waicoolutils.javafx.addListener
import javafx.scene.control.Button
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import org.controlsfx.control.PrefixSelectionComboBox
import tornadofx.*

class DraggersView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/profile/combat/draggers.fxml")
    private val draggerSlotGroup: ToggleGroup by fxid()
    private val doll1NameComboBox: PrefixSelectionComboBox<TDoll> by fxid()
    private val doll2NameComboBox: PrefixSelectionComboBox<TDoll> by fxid()

    private val swapButton: Button by fxid()

    override fun onDock() {
        super.onDock()
        swapButton.setOnAction { swapDolls() }
    }

    override fun setValues() {
        val converter = object : StringConverter<TDoll>() {
            override fun toString(td: TDoll) = td.id
            override fun fromString(s: String) = TDoll.lookup(wconfig, s)
        }
        val dolls = TDoll.listAll(wconfig).sortedBy { it.name }
        doll1NameComboBox.items.setAll(dolls)
        doll2NameComboBox.items.setAll(dolls)
        doll1NameComboBox.converter = converter
        doll2NameComboBox.converter = converter
        doll1NameComboBox.typingDelay = 1000
        doll2NameComboBox.typingDelay = 1000
    }

    override fun createBindings() {
        with(profile.combat) {
            val doll1 = draggers[0]!!
            val doll2 = draggers[1]!!

            doll1NameComboBox.selectionModel.select(TDoll.lookup(wconfig, doll1.id))
            doll2NameComboBox.selectionModel.select(TDoll.lookup(wconfig, doll2.id))
            draggerSlotGroup.selectToggle(draggerSlotGroup.toggles.find {
                it.userData.toString().toInt() == draggerSlot
            })
        }
        with(profile.combat) {
            draggerSlotProperty.bind(
                draggerSlotGroup.selectedToggleProperty().integerBinding {
                    it?.userData?.toString()?.toInt() ?: 2
                }
            )
            doll1NameComboBox.selectionModel.selectedItemProperty()
                .addListener("Doll1NameListener") { tdoll ->
                    draggers[0].id = tdoll.id
                }
            doll2NameComboBox.selectionModel.selectedItemProperty()
                .addListener("Doll2NameListener") { tdoll ->
                    draggers[1].id = tdoll.id
                }
        }
    }

    private fun swapDolls() {
        profile.combat.apply {
            draggers[1] = draggers[0].also { draggers[0] = draggers[1] }
        }
        createBindings()
    }
}
