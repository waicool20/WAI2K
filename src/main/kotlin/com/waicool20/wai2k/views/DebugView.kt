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

import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.sikuli.script.ImagePath
import org.sikuli.script.Pattern
import java.nio.file.Files
import java.nio.file.Paths

class DebugView : CoroutineScopeView() {
    override val root: VBox by fxml("/views/debug.fxml")
    private val openButton: Button by fxid()
    private val testButton: Button by fxid()
    private val pathField: TextField by fxid()

    private val wai2KContext: Wai2KContext by inject()

    private val logger = loggerFor<DebugView>()

    init {
        title = "WAI2K - Debugging tools"
    }

    override fun onDock() {
        super.onDock()
        openButton.setOnAction { openPath() }
        testButton.setOnAction { testPath() }
        assetOCRButton.setOnAction { doAssetOCR() }
    }

    private fun openPath() {
        FileChooser().apply {
            title = "Open path to an asset..."
            pathField.text.takeIf { it.isNotBlank() }?.let { initialDirectory = Paths.get(it).parent.toFile() }
            extensionFilters.add(FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"))
            showOpenDialog(null)?.let {
                pathField.text = it.path
            }
        }
    }

    private fun testPath() {
        launch(Dispatchers.IO) {
            wai2KContext.apply {
                val path = Paths.get(pathField.text)
                if (Files.exists(path)) {
                    logger.info("Finding $path")
                    ImagePath.add(path.parent.toString())
                    val device = wai2KContext.adbServer.listDevices(true).find { it.adbSerial == wai2KConfig.lastDeviceSerial }
                    if (device == null) {
                        logger.warn("Could not find device!")
                        return@launch
                    }
                    // Set similarity to 0.1f to make sikulix report the similarity value down to 0.6
                    device.screen.findAllOrEmpty(Pattern(path.fileName.toString()).similar(0.6f))
                            .takeIf { it.isNotEmpty() }
                            ?.forEach {
                                logger.info("Found ${path.fileName}: $it")
                            } ?: run { logger.warn("Could not find the asset anywhere") }
                    ImagePath.remove(path.parent.toString())
                } else {
                    logger.warn("That asset doesn't exist!")
                }
            }
        }
    }
}