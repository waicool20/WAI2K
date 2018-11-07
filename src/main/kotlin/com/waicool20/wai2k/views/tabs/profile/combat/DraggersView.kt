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

import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.game.DollType
import com.waicool20.wai2k.util.Binder
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.javafx.listen
import com.waicool20.waicoolutils.javafx.bind
import javafx.scene.control.ComboBox
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import org.controlsfx.control.Rating
import tornadofx.*

class DraggersView : View(), Binder {
    override val root: VBox by fxml("/views/tabs/profile/combat/draggers.fxml")
    private val doll1NameTextField: TextField by fxid()
    private val doll1StarsRating: Rating by fxid()
    private val doll1LevelSpinner: Spinner<Int> by fxid()
    private val doll1TypeComboBox: ComboBox<DollType> by fxid()

    private val doll2NameTextField: TextField by fxid()
    private val doll2StarsRating: Rating by fxid()
    private val doll2LevelSpinner: Spinner<Int> by fxid()
    private val doll2TypeComboBox: ComboBox<DollType> by fxid()

    private val context: Wai2KContext by inject()

    override fun onDock() {
        super.onDock()
        setValues()
        createBindings()
        context.currentProfileProperty.listen { createBindings() }
    }

    fun setValues() {
        IntegerSpinnerValueFactory(1, 100).let {
            doll1LevelSpinner.valueFactory = it
            doll2LevelSpinner.valueFactory = it
        }
        doll1TypeComboBox.items.setAll(DollType.values().toList())
        doll2TypeComboBox.items.setAll(DollType.values().toList())

        doll1StarsRating.rating = context.currentProfile.combat.doll1Stars.toDouble()
        doll2StarsRating.rating = context.currentProfile.combat.doll2Stars.toDouble()
    }

    override fun createBindings() {
        with (context.currentProfile.combat) {
            doll1NameTextField.bind(doll1NameProperty)
            doll2NameTextField.bind(doll2NameProperty)

            doll1StarsRating.ratingProperty().addListener("Doll1StarsRatingListener") { newVal ->
                doll1StarsProperty.value = newVal.toInt()
            }
            doll2StarsRating.ratingProperty().addListener("Doll2StarsRatingListener") { newVal ->
                doll2StarsProperty.value = newVal.toInt()
            }

            doll1LevelSpinner.bind(doll1LevelProperty)
            doll2LevelSpinner.bind(doll2LevelProperty)

            doll1TypeComboBox.bind(doll1TypeProperty)
            doll2TypeComboBox.bind(doll2TypeProperty)
        }
    }
}