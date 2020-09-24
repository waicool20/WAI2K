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

import com.waicool20.wai2k.game.CombatMap
import com.waicool20.wai2k.script.modules.combat.MapRunner
import com.waicool20.wai2k.views.tabs.profile.AbstractProfileView
import com.waicool20.waicoolutils.javafx.bind
import com.waicool20.waicoolutils.javafx.cellfactories.NoneSelectableCellFactory
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory
import javafx.scene.layout.VBox
import tornadofx.*

class CombatView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/profile/combat/combat.fxml")
    private val enabledCheckBox: CheckBox by fxid()
    private val mapComboBox: ComboBox<String> by fxid()
    private val repairThresholdSpinner: Spinner<Int> by fxid()
    private val enableOneClickCheckBox: CheckBox by fxid()

    override fun setValues() {
        mapComboBox.cellFactory = NoneSelectableCellFactory(Regex("--.+?--"))
        if (mapComboBox.items.isEmpty()) {
            val comparator = compareBy(String::length).then(naturalOrder())
            val storyMaps = MapRunner.list.keys.filterIsInstance<CombatMap.StoryMap>()
            val eventMaps = MapRunner.list.keys.filterIsInstance<CombatMap.EventMap>()
            mapComboBox.items.apply {
                add("-- Normal --")
                addAll(storyMaps.filter { it.type == CombatMap.Type.NORMAL }.map { it.name }.sortedWith(comparator))
                add("-- Emergency --")
                addAll(storyMaps.filter { it.type == CombatMap.Type.EMERGENCY }.map { it.name }.sortedWith(comparator))
                add("-- Night Battle --")
                addAll(storyMaps.filter { it.type == CombatMap.Type.NIGHT }.map { it.name }.sortedWith(comparator))
                add("-- Event --")
                addAll(eventMaps.map { it.name }.sortedWith(comparator))
            }
        }
        repairThresholdSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100)
    }

    override fun createBindings() {
        context.currentProfile.combat.apply {
            enabledCheckBox.bind(enabledProperty)
            mapComboBox.bind(mapProperty)
            repairThresholdSpinner.bind(repairThresholdProperty)
            enableOneClickCheckBox.bind(repairOneClickProperty)
        }
    }
}