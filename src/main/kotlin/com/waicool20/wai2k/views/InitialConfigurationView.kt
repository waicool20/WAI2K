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
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.waicoolutils.DesktopUtils
import com.waicool20.waicoolutils.javafx.AlertFactory
import com.waicool20.waicoolutils.javafx.listen
import com.waicool20.waicoolutils.javafx.listenDebounced
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import tornadofx.*
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class InitialConfigurationView : View() {
    override val root: VBox by fxml("/views/initial-config.fxml")

    private val sikulixContent: VBox by fxid()
    private val ocrContent: VBox by fxid()

    private val pathTextField: TextField by fxid()
    private val chooseButton: Button by fxid()

    private val requiredFilesVBox: VBox by fxid()
    private val ocrPathLink: Hyperlink by fxid()

    private val configs: Configurations by inject()
    private val ocrDir = configs.wai2KConfig.ocrDirectory

    init {
        title = "WAI2K - Initial Configuration"
    }

    override fun onDock() {
        super.onDock()
        currentStage?.isResizable = false
        pathTextField.textProperty().apply {
            listen { pathTextField.styleClass.setAll("unsure") }
            listenDebounced(1000, "InitialConfig-path") { newVal ->
                configs.wai2KConfig.apply {
                    sikulixJarPath = Paths.get(newVal)
                    if (sikulixJarIsValid()) {
                        pathTextField.styleClass.setAll("valid")
                    } else {
                        pathTextField.styleClass.setAll("invalid")
                    }
                }
                checkConfig()
            }
        }
        addRequiredOcrFilesHyperlinks()
        configs.wai2KConfig.apply {
            if (sikulixJarIsValid()) sikulixContent.removeFromParent()
            if (ocrIsValid()) ocrContent.removeFromParent()
        }
        chooseButton.action(::chooseSikulixPath)
        monitorOcrFiles()
    }

    override fun onUndock() {
        super.onUndock()
        if (configs.wai2KConfig.isValid) {
            configs.wai2KConfig.save()
        } else {
            exitProcess(0)
        }
    }

    private fun checkConfig() {
        if (configs.wai2KConfig.isValid) runLater { close() }
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

    private fun addRequiredOcrFilesHyperlinks() {
        requiredFilesVBox.apply {
            Wai2KConfig.requiredOcrFiles.filterNot { Files.exists(ocrDir.resolve(it)) }.forEach {
                hbox(spacing = 10, alignment = Pos.CENTER_LEFT) {
                    label("-")
                    hyperlink(it) {
                        styleClass.add("link")
                        action {
                            DesktopUtils.browse("https://github.com/tesseract-ocr/tessdata/blob/master/$it?raw=true")
                        }
                    }
                }
            }
        }
    }

    private fun monitorOcrFiles() = try {
        if (Files.notExists(ocrDir)) Files.createDirectories(ocrDir)
        ocrPathLink.apply {
            text = "$ocrDir"
            action { DesktopUtils.open(ocrDir) }
        }
        val ws = FileSystems.getDefault().newWatchService()
        val watchKey = ocrDir.register(ws, arrayOf(ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE))
        thread(name = "OCR Directory Watcher Thread") {
            while (true) {
                val key = ws.take()
                key.pollEvents().filterNot { it.kind() == OVERFLOW }.forEach {
                    if (configs.wai2KConfig.ocrIsValid()) watchKey.cancel()
                    checkConfig()
                }
                if (!key.reset()) break
            }
        }
    } catch (e: Exception) {
        AlertFactory.error(
                content = "Error occurred while checking ocr files, try again later: ${e.message}"
        ).showAndWait()
        exitProcess(0)
    }
}
