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

import com.sun.javafx.css.StyleManager
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.views.Wai2KWorkspace
import com.waicool20.waicoolutils.javafx.addListener
import javafx.application.Application
import javafx.scene.layout.VBox
import org.controlsfx.control.ToggleSwitch
import tornadofx.*

class AppearancePrefView : View() {
    override val root: VBox by fxml("/views/tabs/preferences/appearance.fxml")
    private val darkModeToggleSwitch: ToggleSwitch by fxid()

    private val context: Wai2KContext by inject()

    override fun onDock() {
        super.onDock()
        darkModeToggleSwitch.isSelected = context.wai2KConfig.appearanceConfig.darkMode

        darkModeToggleSwitch.selectedProperty().addListener("DarkModeToggle") { newVal ->
            Wai2KWorkspace.setDarkMode(newVal)
            context.wai2KConfig.appearanceConfig.darkMode = newVal
        }
    }
}