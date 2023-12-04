/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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
import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.util.Binder
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.javafx.addListener
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.control.Button
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
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
    private val ipButton: Button by fxid()
    private val touchesButton: Button by fxid()
    private val pointerButton: Button by fxid()
    private val takeScreenshotButton: Button by fxid()
    private val captureSeriesButton: Button by fxid()
    private val testLatencyButton: Button by fxid()

    private val deviceView: ImageView by fxid()
    private var renderJob: Job? = null
    private var capturingJob: Job? = null
    private var lastDir: File? = null

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
        ipButton.setOnAction { connectByIP() }
        deviceComboBox.converter = object : StringConverter<AndroidDevice>() {
            override fun toString(device: AndroidDevice?) =
                device?.properties?.name ?: "No device selected"

            override fun fromString(string: String) = null
        }

        if (Wai2k.config.lastDeviceSerial.contains(Regex("(?:\\d{1,3}\\.){3}\\d{1,3}"))) {
            ADB.connect(Wai2k.config.lastDeviceSerial) { device ->
                launch {
                    if (device != null) deviceComboBox.selectionModel.select(device)
                }
            }
        } else {
            refreshDeviceLists { list ->
                list.find { it.serial == Wai2k.config.lastDeviceSerial }?.let {
                    launch { deviceComboBox.selectionModel.select(it) }
                }
            }
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
            val device = deviceComboBox.selectedItem ?: return@action
            val outputDir = Wai2k.CONFIG_DIR.resolve("screenshots").createDirectories()
            FileChooser().apply {
                title = "Save screenshot to?"
                initialDirectory = outputDir.toFile()
                initialFileName = "${
                    DateTimeFormatter
                        .ofPattern("yyyy-MM-dd HH-mm-ss")
                        .format(LocalDateTime.now())
                }.png"
                extensionFilters.add(FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"))
                showSaveDialog(null)?.let { file ->
                    launch(Dispatchers.IO) {
                        ImageIO.write(device.displays.first().capture().img, "PNG", file)
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
                        ImageIO.write(device.displays.first().capture().img, "PNG", out)
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
                withContext(Dispatchers.Main) {
                    deviceView.image = null
                }
                val device = deviceComboBox.selectedItem ?: return@launch
                logger.info("Testing capture latency")
                val times = 10
                var total = 0L
                repeat(times) {
                    val time = measureTimeMillis { device.displays.first().capture() }
                    delay(100)
                    logger.info("Capture $it: $time ms")
                    total += time
                }
                logger.info("Average: ${total / times} ms")
                createNewRenderJob(device)
            }
        }
    }

    override fun onTabSelected() {
        super.onTabSelected()
        createNewRenderJob(deviceComboBox.selectedItem ?: return)
    }

    private fun connectByIP() {
        dialog {
            stage.isResizable = false
            hbox {
                label("IP: ")
                val t = textfield("127.0.0.1:62001").apply {
                    promptText = "127.0.0.1:62001"
                }
                button("Connect") {
                    setOnAction {
                        ADB.connect(t.text) { device ->
                            launch {
                                if (device != null) deviceComboBox.selectionModel.select(device)
                            }
                        }
                        close()
                    }
                }
            }.apply {
                alignment = Pos.CENTER
                spacing = 5.0
            }
        }
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
            with(Wai2k.config) {
                setNewDevice(device)
                save()
            }
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
                    val image = device.displays.first().lastCapture.img.takeIf {
                        System.currentTimeMillis() - lastCaptureTime < 3000
                    } ?: run {
                        lastCaptureTime = System.currentTimeMillis()
                        device.displays.first().capture().img
                    }
                    withContext(Dispatchers.Main) {
                        deviceView.image = SwingFXUtils.toFXImage(image, null)
                    }
                } catch (e: AndroidDevice.UnexpectedDisconnectException) {
                    logger.warn("Device $device disconnected")
                    break
                } catch (e: Exception) {
                    logger.warn("Failed to get frame for device $device", e)
                }
            }
        }
    }
}
