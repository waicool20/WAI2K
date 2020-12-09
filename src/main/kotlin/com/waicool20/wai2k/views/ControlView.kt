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

import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import javafx.beans.property.IntegerProperty
import javafx.beans.property.StringProperty
import javafx.scene.control.Button
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.StageStyle
import javafx.util.StringConverter
import kotlinx.coroutines.*
import org.controlsfx.control.ToggleSwitch
import tornadofx.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt


class ControlView : CoroutineScopeView() {
    override val root: VBox by fxml("/views/control.fxml")

    class HotKey(key: String = "", x: Int = 0, y: Int = 0, variance: Int = 10) {
        val keyProperty: StringProperty = key.toProperty()
        val xProperty: IntegerProperty = x.toProperty()
        val yProperty: IntegerProperty = y.toProperty()
        val varianceProperty: IntegerProperty = variance.toProperty()

        val key by keyProperty
        val x by xProperty
        val y by yProperty
        val variance by varianceProperty
    }

    private val enableToggleSwitch: ToggleSwitch by fxid()

    private val tableView: TableView<HotKey> by fxid()
    private val keyColumn: TableColumn<HotKey, String> by fxid()
    private val xColumn: TableColumn<HotKey, Int> by fxid()
    private val yColumn: TableColumn<HotKey, Int> by fxid()
    private val varianceColumn: TableColumn<HotKey, Int> by fxid()

    private val editHBox: HBox by fxid()
    private val setKeyButton: Button by fxid()
    private val setCoordButton: Button by fxid()
    private val deleteButton: Button by fxid()
    private val addButton: Button by fxid()

    private val context: Wai2KContext by inject()

    private var device: AndroidDevice? = null
    private val hotkeysMap = mutableMapOf<HotKey, Int>()

    override fun onDock() {
        super.onDock()
        title = "Control"
        currentStage?.isResizable = false

        keyColumn.cellValueFactory = PropertyValueFactory("key")
        xColumn.cellValueFactory = PropertyValueFactory("x")
        yColumn.cellValueFactory = PropertyValueFactory("y")
        varianceColumn.cellValueFactory = PropertyValueFactory("variance")
        varianceColumn.cellFactory =
            TextFieldTableCell.forTableColumn(object : StringConverter<Int>() {
                override fun toString(i: Int?): String = i.toString()
                override fun fromString(s: String?): Int? = s?.toIntOrNull()
            })
        varianceColumn.setOnEditCommit { it.rowValue.varianceProperty.set(it.newValue) }

        editHBox.disableProperty().bind(tableView.selectionModel.selectedItemProperty().isNull)
        setCoordButton.action {
            val device = ADB.getDevices().find { it.serial == context.wai2KConfig.lastDeviceSerial }
                ?: return@action
            val touches = device.input.touchInterface?.touches ?: return@action
            launch {
                var job: Job? = null
                dialog("Tap on screen", Modality.WINDOW_MODAL, StageStyle.UNDECORATED) {
                    job = launch(Dispatchers.IO) {
                        var oldX = touches[0].cursorX
                        var oldY = touches[0].cursorY
                        while (isActive) {
                            val newX = touches[0].cursorX
                            val newY = touches[0].cursorY
                            if ((newX != oldX || newY != oldY) && touches[0].isTouching) {
                                withContext(Dispatchers.Main) {
                                    tableView.selectedItem?.xProperty?.set(newX)
                                    tableView.selectedItem?.yProperty?.set(newY)
                                    tableView.refresh()
                                    close()
                                }
                                break
                            }
                            oldX = newX
                            oldY = newY
                            delay(10)
                        }
                    }
                }
                job?.join()
            }

        }
        setKeyButton.action {
            waitForKeyPress {
                if (it == null) {
                    tableView.items.remove(tableView.selectedItem)
                } else {
                    tableView.selectedItem?.keyProperty?.set(it.getName())
                    tableView.refresh()
                }
            }
        }
        deleteButton.action { tableView.items.removeAll(tableView.selectionModel.selectedItems) }
        addButton.action {
            waitForKeyPress {
                if (it != null) {
                    tableView.items.add(HotKey(it.getName()))
                    tableView.refresh()
                }
            }
        }
        createNewControlWindow()
    }

    private fun waitForKeyPress(action: (KeyCode?) -> Unit) {
        dialog("Press a key", Modality.WINDOW_MODAL, StageStyle.UNDECORATED) {
            scene.setOnKeyPressed {
                if (it.code == KeyCode.ESCAPE) {
                    action(null)
                } else {
                    action(it.code)
                }
                close()
            }
        }
    }

    private fun createNewControlWindow() {
        val d = ADB.getDevices().find { it.serial == context.wai2KConfig.lastDeviceSerial }
        if (d == null) {
            enableToggleSwitch.isSelected = false
            return
        }
        val ti = d.input.touchInterface ?: return
        dialog("Screen Protector", Modality.NONE, StageStyle.DECORATED, owner = null) {}?.apply {
            isResizable = true
            width = 400.0
            height = 400.0
            opacity = 0.8

            var x = 0
            var y = 0
            val mouseDown = AtomicBoolean(false)
            val j = launch(Dispatchers.IO) {
                while (isActive) {
                    if (mouseDown.get()) ti.touchMove(0, x, y)
                    delay(10)
                }
            }
            setOnHiding { }

            scene.apply {
                root = hbox { }
                root.style = "-fx-background-color: rgba(0, 0, 0, 0);"
                setOnScroll {
                    opacity = if (it.deltaY > 0) {
                        0.8
                    } else {
                        (opacity - 0.05).coerceAtLeast(0.01)
                    }
                }
                setOnMouseMoved {
                    x = ((it.x / width) * d.properties.displayWidth).roundToInt()
                    y = ((it.y / height) * d.properties.displayHeight).roundToInt()
                }
                setOnMouseDragged {
                    x = ((it.x / width) * d.properties.displayWidth).roundToInt()
                    y = ((it.y / height) * d.properties.displayHeight).roundToInt()
                }
                setOnMousePressed {
                    launch(Dispatchers.IO) {
                        ti.touchMove(0, x, y)
                        ti.touchDown(0)
                        ti.eventSync()
                        mouseDown.set(true)
                    }
                }
                setOnMouseReleased {
                    launch(Dispatchers.IO) {
                        mouseDown.set(false)
                        ti.touchUp(0)
                        ti.eventSync()
                    }
                }
                setOnKeyPressed { ke ->
                    val hotkey = tableView.items.find { it.key == ke.code.getName() }
                        ?: return@setOnKeyPressed
                    if (hotkeysMap.containsKey(hotkey)) return@setOnKeyPressed
                    val slot = (ti.touches.drop(1).first { !it.isTouching }?.slot
                        ?: return@setOnKeyPressed)
                    hotkeysMap[hotkey] = slot
                    ti.touchMove(slot, hotkey.x, hotkey.y)
                    ti.touchDown(slot)
                    ti.eventSync()
                }
                setOnKeyReleased { ke ->
                    val hotkey = tableView.items.find { it.key == ke.code.getName() }
                        ?: return@setOnKeyReleased
                    val slot = hotkeysMap[hotkey] ?: return@setOnKeyReleased
                    ti.touchUp(slot)
                    ti.eventSync()
                    hotkeysMap.remove(hotkey)
                }
            }
        }
    }
}
