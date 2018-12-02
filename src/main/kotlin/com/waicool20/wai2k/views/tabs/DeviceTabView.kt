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

package com.waicool20.wai2k.views.tabs

import com.waicool20.wai2k.android.AndroidDevice
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.util.Binder
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.util.StringConverter
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import javax.imageio.ImageIO
import kotlin.concurrent.thread

class DeviceTabView : View(), Binder {
    override val root: VBox by fxml("/views/tabs/device-tab.fxml")
    private val androidVersionLabel: Label by fxid()
    private val brandLabel: Label by fxid()
    private val manufacturerLabel: Label by fxid()
    private val modelLabel: Label by fxid()
    private val serialLabel: Label by fxid()
    private val displayLabel: Label by fxid()
    private val deviceComboBox: ComboBox<AndroidDevice> by fxid()
    private val reloadDevicesButton: Button by fxid()
    private val pointerButton: Button by fxid()
    private val takeScreenshotButton: Button by fxid()

    private val deviceView: ImageView by fxid()
    private var renderJob: Job? = null

    private val context: Wai2KContext by inject()

    private val logger = loggerFor<DeviceTabView>()

    init {
        title = "Device"

        reloadDevicesButton.graphic = Glyph("FontAwesome", FontAwesome.Glyph.REFRESH)
        reloadDevicesButton.setOnAction {
            refreshDeviceLists()
        }
        deviceComboBox.converter = object : StringConverter<AndroidDevice>() {
            override fun toString(device: AndroidDevice) = device.properties.name
            override fun fromString(string: String) = null
        }
        createBindings()
    }

    override fun createBindings() {
        val itemProp = deviceComboBox.selectionModel.selectedItemProperty()
        androidVersionLabel.textProperty().bind(
                itemProp.stringBinding { it?.properties?.androidVersion ?: "" }
        )
        brandLabel.textProperty().bind(
                itemProp.stringBinding { it?.properties?.brand ?: "" }
        )
        manufacturerLabel.bind(
                itemProp.stringBinding { it?.properties?.manufacturer ?: "" }
        )
        modelLabel.bind(
                itemProp.stringBinding { it?.properties?.model ?: "" }
        )
        serialLabel.bind(
                itemProp.stringBinding { it?.adbSerial ?: "" }
        )
        displayLabel.textProperty().bind(
                itemProp.stringBinding {
                    it?.properties?.run { "${displayWidth}x$displayHeight" } ?: ""
                }
        )
        itemProp.addListener("AndroidDeviceSelection") { newVal ->
            if (newVal != null) {
                logger.debug("Selected device: ${newVal.properties.name}")
                context.wai2KConfig.lastDeviceSerial = newVal.adbSerial
                context.wai2KConfig.save()
                // Cancel the current job before starting a new one
                renderJob?.cancel()
                renderJob = createNewRenderJob(newVal)
            }
        }
        pointerButton.action {
            deviceComboBox.selectedItem?.togglePointerInfo()
        }
        takeScreenshotButton.action {
            deviceComboBox.selectedItem?.let { device ->
                FileChooser().apply {
                    title = "Save screenshot to?"
                    extensionFilters.add(FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"))
                    showSaveDialog(null)?.let { file ->
                        GlobalScope.launch(Dispatchers.IO) { ImageIO.write(device.takeFastScreenshot(), "PNG", file) }
                    }
                }
            }
        }
    }

    override fun onDock() {
        super.onDock()
        refreshDeviceLists { list ->
            list.find { it.adbSerial == context.wai2KConfig.lastDeviceSerial }?.let {
                runLater { deviceComboBox.selectionModel.select(it) }
            }
        }
    }

    private fun refreshDeviceLists(action: (List<AndroidDevice>) -> Unit = {}) {
        thread(name = "Refresh Device List Task") {
            logger.debug("Refreshing device list")
            val serial = deviceComboBox.selectedItem?.adbSerial
            val list = context.adbServer.listDevices()

            logger.debug("Found ${list.size} devices")
            runLater {
                deviceComboBox.items.setAll(list)
                list.find { it.adbSerial == serial }?.let {
                    deviceComboBox.selectionModel.select(it)
                } ?: deviceComboBox.selectionModel.clearSelection()
            }
            action(list)
        }
    }

    private fun createNewRenderJob(device: AndroidDevice) = GlobalScope.launch {
        while (isActive) {
            if (owningTab?.isSelected == true) {
                withContext(Dispatchers.JavaFx) {
                    deviceView.image = SwingFXUtils.toFXImage(device.takeFastScreenshot(), null)
                }
            }
        }
    }
}
