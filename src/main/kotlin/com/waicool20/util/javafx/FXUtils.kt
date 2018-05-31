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

package com.waicool20.util.javafx

import com.sun.javafx.scene.control.skin.TableHeaderRow
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.util.Callback
import javafx.util.Duration
import javafx.util.StringConverter
import org.controlsfx.control.CheckModel
import tornadofx.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.abs

//<editor-fold desc="Extension functions">

private object ListenerTracker {
    val listeners = mutableMapOf<String, ChangeListener<*>>()
}


/**
 * Extension function for adding listener with a given name, guaranteed to have only one listener
 * per name.
 *
 * @param name Unique name of listener
 * @param listener Listener, receives (ObservableValue, Old Value, New Value)
 */
@Suppress("UNCHECKED_CAST")
fun <T> ObservableValue<T>.addListener(name: String, listener: (ObservableValue<out T>, T, T) -> Unit) {
    (ListenerTracker.listeners[name] as? ChangeListener<T>)?.let { removeListener(it) }
    val changeListener = ChangeListener(listener)
    ListenerTracker.listeners[name] = changeListener
    addListener(changeListener)
}


/**
 * Extension function for adding listener with a given name, guaranteed to have only one listener
 * per name.
 *
 * @param name Unique name of listener
 * @param listener Listener, receives (New Value)
 */
fun <T> ObservableValue<T>.addListener(name: String, listener: (T) -> Unit) =
        addListener(name) { _, _, newVal -> listener(newVal) }


/**
 * Extension function for adding listener that receives no parameters.
 *
 * @param listener Listener
 */
fun ObservableValue<*>.listen(listener: () -> Unit) = addListener { _, _, _ -> listener() }

//<editor-fold desc="Node Size Utils">

fun Parent.setInitialSceneSizeAsMin() = sceneProperty().setInitialSizeAsMin()
fun ReadOnlyObjectProperty<Scene>.setInitialSizeAsMin() = setInitialSize(null, null, true)
fun Parent.setInitialSceneSize(width: Double, height: Double, asMinimum: Boolean) = sceneProperty().setInitialSize(width, height, asMinimum)

fun ReadOnlyObjectProperty<Scene>.setInitialSize(width: Double?, height: Double?, asMinimum: Boolean) {
    addListener { _, _, newVal ->
        newVal?.windowProperty()?.addListener { _, _, newWindow ->
            newWindow?.addEventFilter(WindowEvent.WINDOW_SHOWN, {
                with(it.target as Stage) {
                    if (width != null && height != null) {
                        this.width = width
                        this.height = height
                    }
                    if (asMinimum) {
                        minHeight = this.height + 25
                        minWidth = this.width + 25
                    }
                }
            })
        }
    }
}

//</editor-fold>

//<editor-fold desc="Table Utils">

fun TableView<*>.lockColumnWidths() {
    columns.addListener(ListChangeListener<TableColumn<*, *>> {
        while (it.next()) {
            it.addedSubList.forEach { it.isResizable = false }
        }
    })
    columns.forEach { it.isResizable = false }
}

fun TableView<*>.disableHeaderMoving() {
    widthProperty().listen {
        val row = lookup("TableHeaderRow") as TableHeaderRow
        row.reorderingProperty().listen { row.isReordering = false }
    }
}

fun TableColumn<*, *>.setWidthRatio(tableView: TableView<*>, ratio: Double) =
        prefWidthProperty().bind(tableView.widthProperty().subtract(20).multiply(ratio))

//</editor-fold>

//<editor-fold desc="Spinner Utils">

private val spinnerWraps = mutableMapOf<Spinner<*>, Boolean>()
fun <T> Spinner<T>.updateOtherSpinnerOnWrap(spinner: Spinner<T>, min: T, max: T) {
    spinnerWraps.putIfAbsent(this, false)
    addEventHandler(MouseEvent.ANY, { event ->
        if (event.eventType == MouseEvent.MOUSE_PRESSED ||
                event.eventType == MouseEvent.MOUSE_RELEASED) {
            if (event.button == MouseButton.PRIMARY) {
                val node = event.target as Node
                if (node is StackPane && node.getParent() is Spinner<*>) {
                    if (node.styleClass.contains("increment-arrow-button") ||
                            node.styleClass.contains("decrement-arrow-button")) {
                        spinnerWraps[this] = event.eventType == MouseEvent.MOUSE_PRESSED
                    }
                }
            }
        }
    })
    valueProperty().addListener { _, oldVal, newVal ->
        if (spinnerWraps[this] == true) {
            if (oldVal == max && newVal == min) {
                spinner.increment()
            } else if (oldVal == min && newVal == max) {
                spinner.decrement()
            }
        }
    }
}

/**
 * Bounds the spinners values based on a given time unit and sets it to wrap around
 *
 * @param unit Time unit to bind the spinner value, eg. [TimeUnit.HOURS] binds it to 0-23
 * @param allowInvalid Allows -1 to be a value
 *
 * @throws IllegalStateException if the time unit is not supported
 */
fun Spinner<Int>.asTimeSpinner(unit: TimeUnit, allowInvalid: Boolean = false) {
    val formatter = object : StringConverter<Int>() {
        override fun toString(i: Int?): String = if (i == null) {
            if (allowInvalid) "-01" else "00"
        } else {
            "${if (i < 0) "-" else ""}${String.format("%02d", abs(i))}"
        }

        override fun fromString(s: String): Int = s.toInt()
    }
    editor.textFormatter = TextFormatter(formatter)
    editor.alignment = Pos.CENTER
    val lowBound = if (allowInvalid) -1 else 0
    valueFactory = when (unit) {
        TimeUnit.DAYS -> SpinnerValueFactory.IntegerSpinnerValueFactory(lowBound, 31)
        TimeUnit.HOURS -> SpinnerValueFactory.IntegerSpinnerValueFactory(lowBound, 23)
        TimeUnit.MINUTES -> SpinnerValueFactory.IntegerSpinnerValueFactory(lowBound, 59)
        TimeUnit.SECONDS -> SpinnerValueFactory.IntegerSpinnerValueFactory(lowBound, 59)
        else -> kotlin.error("TimeUnit $unit is not supported")
    }
    valueFactory.isWrapAround = true
}

//</editor-fold>

//<editor-fold desc="Tooltip Utils">

enum class TooltipSide {
    TOP_LEFT, TOP, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT
}

fun Tooltip.showAt(node: Node, side: TooltipSide = TooltipSide.TOP_RIGHT) {
    with(node) {
        val bounds = localToScene(boundsInLocal)
        val x = when (side) {
            TooltipSide.TOP_LEFT, TooltipSide.CENTER_LEFT, TooltipSide.BOTTOM_LEFT -> bounds.minX
            TooltipSide.TOP, TooltipSide.CENTER, TooltipSide.BOTTOM -> (bounds.minX + bounds.maxX) / 2
            TooltipSide.TOP_RIGHT, TooltipSide.CENTER_RIGHT, TooltipSide.BOTTOM_RIGHT -> bounds.maxX
        }
        val y = when (side) {
            TooltipSide.TOP_LEFT, TooltipSide.TOP, TooltipSide.TOP_RIGHT -> bounds.minY
            TooltipSide.CENTER_LEFT, TooltipSide.CENTER, TooltipSide.CENTER_RIGHT -> (bounds.minY + bounds.maxY) / 2
            TooltipSide.BOTTOM_LEFT, TooltipSide.BOTTOM, TooltipSide.BOTTOM_RIGHT -> bounds.maxY
        }
        show(node, x + scene.window.x, y + scene.window.y)
    }
}

fun Tooltip.fadeAfter(millis: Long) {
    setOnShown {
        opacity = 1.0
        thread {
            TimeUnit.MILLISECONDS.sleep(millis)
            runLater {
                Timeline().apply {
                    keyFrames.add(KeyFrame(Duration.millis(500.0), KeyValue(opacityProperty(), 0)))
                    setOnFinished { hide() }
                }.play()
            }
        }
    }
}

//</editor-fold>

fun Node.getParentTabPane(): TabPane? {
    var parentNode = parent
    while (parentNode != null) {
        if (parentNode is TabPane) {
            return parentNode
        } else {
            parentNode = parentNode.parent
        }
    }
    return null
}

fun TabPane.setSideWithHorizontalText(side: Side, width: Double = 100.0) {
    this.side = side
    if (side == Side.TOP || side == Side.BOTTOM) return
    tabMinHeight = width
    tabMaxHeight = width
    tabs.forEach { tab ->
        var text = tab.text
        if (text == "" && tab.properties.containsKey("text")) {
            text = tab.properties["text"].toString()
        } else {
            tab.properties["text"] = tab.text
        }
        val rotation = if (side == Side.LEFT) 90.0 else -90.0
        val label = Label(text)
        val pane = StackPane(Group(label))
        label.rotate = rotation
        pane.rotate = rotation
        tab.graphic = pane
        tab.text = ""
    }
    isRotateGraphic = true
}

fun <T> CheckModel<T>.checkAll(items: List<T>) {
    clearChecks()
    if (items.isNotEmpty()) items.forEach { check(it) }
}

//</editor-fold>

//<editor-fold desc="Utility classes">

object AlertFactory {
    private fun alert(
            type: Alert.AlertType,
            stage: Stage?,
            title: String,
            header: String?,
            content: String
    ) = Alert(type).apply {
        this.title = title
        this.headerText = header
        this.contentText = content
        setOnHidden { stage?.toFront() }
    }

    fun info(stage: Stage? = null,
             title: String = "Info",
             header: String? = null,
             content: String) =
            alert(Alert.AlertType.INFORMATION, stage, title, header, content)

    fun warn(stage: Stage? = null,
             title: String = "Warning",
             header: String? = null,
             content: String) =
            alert(Alert.AlertType.WARNING, stage, title, header, content)

    fun error(stage: Stage? = null,
              title: String = "Error",
              header: String? = null,
              content: String) =
            alert(Alert.AlertType.ERROR, stage, title, header, content)
}

class EnumCapitalizedNameConverter<T : Enum<*>> : StringConverter<T>() {
    override fun toString(e: T) = e.toString().replace("_", " ").toLowerCase().capitalize()
    override fun fromString(string: String): T? = null
}

//<editor-fold desc="Cell Factories">

class DeselectableCellFactory<T> : Callback<ListView<T>, ListCell<T>> {

    override fun call(viewList: ListView<T>): ListCell<T> {
        val cell = object : ListCell<T>() {
            override fun updateItem(item: T, empty: Boolean) {
                super.updateItem(item, empty)
                text = item?.toString()
            }
        }
        with(cell) {
            addEventFilter(MouseEvent.MOUSE_PRESSED, { event ->
                viewList.requestFocus()
                if (!cell.isEmpty) {
                    val index = cell.index
                    with(viewList.selectionModel) {
                        if (selectedIndices.contains(index)) {
                            clearSelection(index)
                        } else {
                            select(index)
                        }
                    }
                    event.consume()
                }
            })
        }
        return cell
    }
}

class NoneSelectableCellFactory(val regex: Regex) : Callback<ListView<String>, ListCell<String>> {
    override fun call(p0: ListView<String>?): ListCell<String> {
        return object : ListCell<String>() {
            override fun updateItem(item: String?, empty: Boolean) {
                super.updateItem(item, empty)
                if (empty) {
                    text = null
                    isDisabled = false
                }
                if (item != null) {
                    text = item
                    isDisable = item.matches(regex)
                }
            }
        }
    }
}

//</editor-fold>

//<editor-fold desc="Table Columns">

class IndexColumn<T>(text: String = "", start: Int = 0) : TableColumn<T, String>(text) {

    init {
        isSortable = false
        setCellFactory {
            TableCell<T, String>().apply {
                textProperty().bind(javafx.beans.binding.Bindings.`when`(emptyProperty())
                        .then("")
                        .otherwise(indexProperty().add(start).asString()))
            }
        }
    }
}

class OptionsColumn(text: String = "", var options: List<String>, table: TableView<String>,
                    var filter: (cell: TableCell<String, String>, string: String) -> Boolean = { _, _ -> true },
                    var maxRows: Int = Integer.MAX_VALUE) : TableColumn<String, String>(text) {

    init {
        val addText = "<Add Item>"
        setCellFactory {
            ComboBoxTableCell<String, String>().apply {
                converter = object : StringConverter<String>() {
                    override fun toString(string: String?): String {
                        return if (index != table.items.size - 1) {
                            if (string == addText) "" else string ?: ""
                        } else {
                            string ?: ""
                        }
                    }

                    override fun fromString(string: String?): String = ""
                }
                indexProperty().listen {
                    items.setAll(if (index != table.items.size - 1) addText else "")
                    items.addAll(options.filter { filter.invoke(this, it) })
                }
            }
        }
        setOnEditCommit { event ->
            with(table.items) {
                val index = event.tablePosition.row
                if (index != size - 1) {
                    removeAt(index)
                    if (event.newValue != addText) add(index, event.newValue)
                    table.selectionModel.select(index)
                } else {
                    if (event.newValue != addText && index < maxRows && event.newValue != "") {
                        add(size - 1, event.newValue)
                    }
                }
                table.refresh()
                event.consume()
            }
        }
        table.items.addListener(ListChangeListener<String> {
            if (it.next()) {
                if (table.items[table.items.size - 1] != addText) {
                    table.items.add(addText)
                }
            }
        })
        table.sceneProperty().addListener { _, _, newVal ->
            newVal?.windowProperty()?.addListener { _, _, newWindow ->
                newWindow?.addEventFilter(WindowEvent.WINDOW_SHOWN, {
                    if (table.items.size == 0) table.items.add(addText)
                })
            }
        }
    }

}

//</editor-fold>

//</editor-fold>
