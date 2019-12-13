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

import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.waicoolutils.DesktopUtils
import com.waicool20.waicoolutils.javafx.AlertFactory
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tornadofx.*
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardWatchEventKinds.*
import kotlin.system.exitProcess

class InitialConfigurationView : CoroutineScopeView() {
    override val root: VBox by fxml("/views/initial-config.fxml")

    private val ocrContent: VBox by fxid()

    private val requiredFilesVBox: VBox by fxid()
    private val ocrPathLink: Hyperlink by fxid()

    private val context: Wai2KContext by inject()
    private val ocrDir = context.wai2KConfig.ocrDirectory

    init {
        title = "WAI2K - Initial Configuration"
    }

    override fun onDock() {
        super.onDock()
        currentStage?.isResizable = false
        addRequiredOcrFilesHyperlinks()
        context.wai2KConfig.apply {
            if (ocrIsValid()) ocrContent.removeFromParent()
        }
        monitorOcrFiles()
    }

    override fun onUndock() {
        super.onUndock()
        if (context.wai2KConfig.isValid) {
            context.wai2KConfig.save()
        } else {
            exitProcess(0)
        }
    }

    private fun checkConfig() {
        if (context.wai2KConfig.isValid) launch { close() }
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
        currentStage?.sizeToScene()
    }

    private fun monitorOcrFiles() = try {
        if (Files.notExists(ocrDir)) Files.createDirectories(ocrDir)
        ocrPathLink.apply {
            text = "$ocrDir"
            action { DesktopUtils.open(ocrDir) }
        }
        val ws = FileSystems.getDefault().newWatchService()
        val watchKey = ocrDir.register(ws, arrayOf(ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE))
        launch(Dispatchers.Default + CoroutineName("OCR Directory Watcher")) {
            while (true) {
                val key = withContext(Dispatchers.IO) { ws.take() }
                repeat(key.pollEvents().filterNot { it.kind() == OVERFLOW }.size) {
                    if (context.wai2KConfig.ocrIsValid()) watchKey.cancel()
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
