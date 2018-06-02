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

import com.waicool20.wai2k.config.Configurations
import com.waicool20.waicoolutils.javafx.listen
import com.waicool20.waicoolutils.javafx.listenDebounced
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import tornadofx.*
import java.nio.file.Paths
import kotlin.system.exitProcess

class InitialConfigurationView : View() {
    override val root: VBox by fxml("/views/initial-config.fxml")
    private val pathTextField: TextField by fxid()
    private val chooseButton: Button by fxid()

    private val configs: Configurations by inject()

    init {
        title = "WAI2K - Initial Configuration"
        pathTextField.textProperty().apply {
            listen { pathTextField.styleClass.setAll("unsure") }
            listenDebounced(1000, "InitialConfig-path") { newVal ->
                configs.wai2KConfig.apply {
                    sikulixJarPath = Paths.get(newVal)
                    if (isValid) {
                        pathTextField.styleClass.setAll("valid")
                        close()
                    } else {
                        pathTextField.styleClass.setAll("invalid")
                    }
                }
            }
        }

        chooseButton.action(::chooseSikulixPath)
    }

    override fun onUndock() {
        super.onUndock()
        if (configs.wai2KConfig.isValid) {
            configs.wai2KConfig.save()
        } else {
            exitProcess(0)
        }
    }

    private fun chooseSikulixPath() {
        FileChooser().apply {
            title = "Path to Sikulix Jar File..."
            extensionFilters.add(FileChooser.ExtensionFilter("JAR files (*.jar)", "*.jar"))
            showOpenDialog(null)?.let {
                configs.wai2KConfig.sikulixJarPath = it.toPath()
                pathTextField.text = it.path
            }
        }
    }
}
