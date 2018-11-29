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

import com.waicool20.wai2k.game.DollType
import com.waicool20.wai2k.views.tabs.profile.AbstractProfileView
import com.waicool20.waicoolutils.javafx.addListener
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import org.controlsfx.control.Rating
import kotlin.collections.set

class DraggersView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/profile/combat/draggers.fxml")
    private val doll1NameTextField: TextField by fxid()
    private val doll1StarsRating: Rating by fxid()
    private val doll1LevelSpinner: Spinner<Int> by fxid()
    private val doll1TypeComboBox: ComboBox<DollType> by fxid()

    private val doll2NameTextField: TextField by fxid()
    private val doll2StarsRating: Rating by fxid()
    private val doll2LevelSpinner: Spinner<Int> by fxid()
    private val doll2TypeComboBox: ComboBox<DollType> by fxid()

    private val swapButton: Button by fxid()

    override fun onDock() {
        super.onDock()
        swapButton.setOnAction { swapDolls() }
    }

    override fun setValues() {
        doll1LevelSpinner.valueFactory = IntegerSpinnerValueFactory(1, 100)
        doll2LevelSpinner.valueFactory = IntegerSpinnerValueFactory(1, 100)
        doll1TypeComboBox.items.setAll(DollType.values().toList())
        doll2TypeComboBox.items.setAll(DollType.values().toList())
    }

    override fun createBindings() {
        with(context.currentProfile.combat) {
            val doll1 = draggers[1]!!
            val doll2 = draggers[2]!!
            doll1NameTextField.text = doll1.name
            doll1LevelSpinner.valueFactory.value = doll1.level
            doll1StarsRating.rating = doll1.stars.toDouble()
            doll1TypeComboBox.value = doll1.type

            doll2NameTextField.text = doll2.name
            doll2LevelSpinner.valueFactory.value = doll2.level
            doll2StarsRating.rating = doll2.stars.toDouble()
            doll2TypeComboBox.value = doll2.type
        }
        with(context.currentProfile.combat) {
            doll1NameTextField.textProperty().addListener("Doll1NameTextFieldListener") { newVal ->
                draggers[1]?.name = newVal
            }
            doll1LevelSpinner.valueProperty().addListener("Doll1LevelSpinnerListener") { newVal ->
                draggers[1]?.level = newVal
            }
            doll1StarsRating.ratingProperty().addListener("Doll1StarsRatingListener") { newVal ->
                draggers[1]?.stars = newVal.toInt()
            }
            doll1TypeComboBox.valueProperty().addListener("Doll1TypeComboBoxListener") { newVal ->
                draggers[1]?.type = newVal
            }

            doll2NameTextField.textProperty().addListener("Doll2NameTextFieldListener") { newVal ->
                draggers[2]?.name = newVal
            }
            doll2LevelSpinner.valueProperty().addListener("Doll2LevelSpinnerListener") { newVal ->
                draggers[2]?.level = newVal
            }
            doll2StarsRating.ratingProperty().addListener("Doll2StarsRatingListener") { newVal ->
                draggers[2]?.stars = newVal.toInt()
            }
            doll2TypeComboBox.valueProperty().addListener("Doll2TypeComboBoxListener") { newVal ->
                draggers[2]?.type = newVal
            }
        }
    }

    private fun swapDolls() {
        context.currentProfile.combat.apply {
            draggers[2] = draggers[1].also { draggers[1] = draggers[2] }
        }
        createBindings()
    }
}