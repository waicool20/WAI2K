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

import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.util.Binder
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.util.StringConverter
import kotlinx.coroutines.*
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

class DeviceTabView : CoroutineScopeView(), Binder {
    override val root: VBox by fxml("/views/tabs/device-tab.fxml")
    private val androidVersionLabel: Label by fxid()
    private val brandLabel: Label by fxid()
    private val manufacturerLabel: Label by fxid()
    private val modelLabel: Label by fxid()
    private val serialLabel: Label by fxid()
    private val displayLabel: Label by fxid()
    private val deviceComboBox: ComboBox<AndroidDevice> by fxid()
    private val reloadDevicesButton: Button by fxid()
    private val touchesButton: Button by fxid()
    private val pointerButton: Button by fxid()
    private val takeScreenshotButton: Button by fxid()
    private val captureSeriesButton: Button by fxid()
    private val testLatencyButton: Button by fxid()

    private val realtimePreviewCheckbox: CheckBox by fxid()
    private val deviceView: ImageView by fxid()
    private var renderJob: Job? = null
    private var capturingJob: Job? = null
    private var lastDir: File? = null

    private val context: Wai2KContext by inject()

    private val logger = loggerFor<DeviceTabView>()

    override fun onDock() {
        super.onDock()
        title = "Device"

        reloadDevicesButton.graphic = Glyph("FontAwesome", FontAwesome.Glyph.REFRESH)
        reloadDevicesButton.setOnAction {
            refreshDeviceLists()
            if (renderJob?.isActive == false) {
                createNewRenderJob(deviceComboBox.selectedItem ?: return@setOnAction)
            }
        }
        deviceComboBox.converter = object : StringConverter<AndroidDevice>() {
            override fun toString(device: AndroidDevice?) = device?.properties?.name ?: "No device selected"
            override fun fromString(string: String) = null
        }

        refreshDeviceLists { list ->
            list.find { it.serial == context.wai2KConfig.lastDeviceSerial }?.let {
                launch { deviceComboBox.selectionModel.select(it) }
            }
        }
        createBindings()
        context.wai2KConfigProperty.addListener("DeviceTabViewConfigListener") { _ -> createBindings() }
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
            itemProp.stringBinding { it?.serial ?: "" }
        )
        displayLabel.textProperty().bind(
            itemProp.stringBinding {
                it?.properties?.run { "${displayWidth}x$displayHeight" } ?: ""
            }
        )
        itemProp.addListener("AndroidDeviceSelection", ::setNewDevice)
        touchesButton.action {
            deviceComboBox.selectedItem?.toggleTouches()
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
                        launch(Dispatchers.IO) { ImageIO.write(device.screens[0].capture(), "PNG", file) }
                    }
                }
            }
        }
        captureSeriesButton.action {
            val device = deviceComboBox.selectedItem ?: return@action
            if (capturingJob == null) {
                val dir = DirectoryChooser().apply {
                    title = "Save screenshots to?"
                    lastDir?.let { initialDirectory = it }
                }.showDialog(null) ?: return@action
                lastDir = dir
                captureSeriesButton.text = "Capture Series Stop"
                capturingJob = launch(Dispatchers.IO) {
                    while (isActive) {
                        val out = dir.resolve("${System.currentTimeMillis()}.png")
                        ImageIO.write(device.screens[0].capture(), "PNG", out)
                        logger.info("Saved $out")
                    }
                }
            } else {
                capturingJob?.cancel()
                capturingJob = null
                captureSeriesButton.text = "Capture Series Start"
            }
        }
        testLatencyButton.action {
            launch(Dispatchers.IO) {
                renderJob?.cancelAndJoin()
                val device = deviceComboBox.selectedItem ?: return@launch
                logger.info("Testing capture latency")
                val times = 10
                var total = 0L
                repeat(times) {
                    val time = measureTimeMillis { device.screens[0].capture() }
                    delay(100)
                    logger.info("Capture $it: $time ms")
                    total += time
                }
                logger.info("Average: ${total / times} ms")
                createNewRenderJob(device)
            }
        }
        context.wai2KConfig.scriptConfig.fastScreenshotModeProperty.addListener("DeviceTabViewFSMListener") { newVal ->
            deviceComboBox.selectedItem?.screens?.firstOrNull()?.fastCaptureMode = newVal
        }
    }

    override fun onTabSelected() {
        super.onTabSelected()
        createNewRenderJob(deviceComboBox.selectedItem ?: return)
    }

    private fun refreshDeviceLists(action: (List<AndroidDevice>) -> Unit = {}) {
        launch(Dispatchers.IO + CoroutineName("Refresh Device List Task")) {
            logger.debug("Refreshing device list")
            val serial = deviceComboBox.selectedItem?.serial
            val list = ADB.getDevices()

            logger.debug("Found ${list.size} devices")
            withContext(Dispatchers.Main) {
                deviceComboBox.items.setAll(list)
                list.find { it.serial == serial }?.let {
                    deviceComboBox.selectionModel.select(it)
                } ?: deviceComboBox.selectionModel.clearSelection()
            }
            action(list)
        }
    }

    private fun setNewDevice(device: AndroidDevice?) {
        if (device != null) {
            logger.debug("Selected device: ${device.properties.name}")
            context.wai2KConfig.lastDeviceSerial = device.serial
            context.wai2KConfig.save()
            device.screens[0].fastCaptureMode = context.wai2KConfig.scriptConfig.fastScreenshotMode
            // Cancel the current job before starting a new one
            createNewRenderJob(device)
        }
    }

    private fun createNewRenderJob(device: AndroidDevice) {
        renderJob?.cancel()
        renderJob = launch(Dispatchers.IO + CoroutineName("Device Tab Render Job")) {
            var lastCaptureTime = System.currentTimeMillis()
            while (isActive && owningTab?.isSelected == true) {
                try {
                    val image = if (realtimePreviewCheckbox.isSelected && device.screens[0].fastCaptureMode) {
                        device.screens[0].capture()
                    } else {
                        device.screens[0].getLastScreenCapture()?.takeIf {
                            System.currentTimeMillis() - lastCaptureTime < 3000
                        } ?: run {
                            lastCaptureTime = System.currentTimeMillis()
                            device.screens[0].capture()
                        }
                    }
                    withContext(Dispatchers.Main) {
                        deviceView.image = SwingFXUtils.toFXImage(image, null)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to get frame for device $device", e)
                    break
                }
            }
        }
    }
}
