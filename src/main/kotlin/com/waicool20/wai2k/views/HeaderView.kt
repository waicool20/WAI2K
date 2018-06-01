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

package com.waicool20.wai2k.views

import com.waicool20.util.javafx.TooltipSide
import com.waicool20.util.javafx.fadeAfter
import com.waicool20.util.javafx.showAt
import com.waicool20.wai2k.config.Configurations
import com.waicool20.wai2k.config.Wai2KProfile
import javafx.scene.control.ComboBox
import javafx.scene.control.SplitMenuButton
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import tornadofx.*
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.streams.toList

class HeaderView : View() {
    override val root: HBox by fxml("/views/header.fxml")
    private val profileComboBox: ComboBox<String> by fxid()
    private val startStopButton: SplitMenuButton by fxid()

    private val configs: Configurations by inject()

    init {
        profileComboBox.setOnShowing { updateProfileItems() }
        profileComboBox.setOnAction { selectProfile() }
        createBindings()
    }

    private fun createBindings() {
        profileComboBox.bind(configs.currentProfile.nameProperty)
    }

    private fun selectProfile() {
        val newProfile = profileComboBox.value
        thread {
            Wai2KProfile.load(newProfile).let {
                configs.apply {
                    wai2KConfig.currentProfile = it.name
                    wai2KConfig.save()
                    currentProfile = it
                }
                runLater {
                    createBindings()
                    Tooltip("Profile ${it.name} has been loaded!").apply {
                        fadeAfter(700)
                        showAt(profileComboBox, TooltipSide.TOP_LEFT)
                    }
                }
            }
        }
    }

    private fun updateProfileItems() {
        val currentProfile = profileComboBox.value
        val profiles = Files.walk(Wai2KProfile.PROFILE_DIR).toList()
                .filter { Files.isRegularFile(it) }
                .map { "${it.fileName}".removeSuffix(".yml") }
                .filter { it != currentProfile }
                .sorted()
        if (profiles.isNotEmpty()) {
            profileComboBox.items.setAll(profiles)
        }
    }
}
