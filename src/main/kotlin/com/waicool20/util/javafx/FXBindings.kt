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

import javafx.beans.binding.*
import javafx.beans.property.*
import javafx.collections.ListChangeListener
import javafx.scene.control.ComboBox
import javafx.scene.control.Spinner
import org.controlsfx.control.CheckComboBox

private object Bindings {
    val objectBindings = mutableMapOf<ObjectProperty<*>, MutableList<ObjectProperty<*>>>()
    val checkComboBoxBindings = mutableMapOf<String, CheckComboBoxBinding<*>>()
    val anyBinding = mutableListOf<Binding<*>>()
}

fun BooleanBinding.persist() = also { Bindings.anyBinding.add(this) }
fun IntegerBinding.persist() = also { Bindings.anyBinding.add(this) }
fun LongBinding.persist() = also { Bindings.anyBinding.add(this) }
fun FloatBinding.persist() = also { Bindings.anyBinding.add(this) }
fun DoubleBinding.persist() = also { Bindings.anyBinding.add(this) }

fun Spinner<Int>.bind(integerProperty: IntegerProperty, readOnly: Boolean = false) =
        bind(valueFactory.valueProperty(), integerProperty.asObject(), readOnly)

fun Spinner<Float>.bind(floatProperty: FloatProperty, readOnly: Boolean = false) =
        bind(valueFactory.valueProperty(), floatProperty.asObject(), readOnly)

fun Spinner<Double>.bind(doubleProperty: DoubleProperty, readOnly: Boolean = false) =
        bind(valueFactory.valueProperty(), doubleProperty.asObject(), readOnly)

fun ComboBox<Int>.bind(integerProperty: IntegerProperty, readOnly: Boolean = false) =
        bind(valueProperty(), integerProperty.asObject(), readOnly)

fun ComboBox<Float>.bind(floatProperty: FloatProperty, readOnly: Boolean = false) =
        bind(valueProperty(), floatProperty.asObject(), readOnly)

fun ComboBox<Double>.bind(doubleProperty: DoubleProperty, readOnly: Boolean = false) =
        bind(valueProperty(), doubleProperty.asObject(), readOnly)

fun <T> CheckComboBox<T>.bind(
        listProperty: ListProperty<T>,
        readOnly: Boolean = false,
        onChange: (ListChangeListener.Change<out T>) -> Unit = {}) {
    with(Bindings.checkComboBoxBindings) {
        get(id)?.unbind()
        remove(id)
        put(id, CheckComboBoxBinding(this@bind, listProperty, readOnly, onChange))
    }
}

private fun <T> bind(objectProperty: ObjectProperty<T>, objectProperty1: ObjectProperty<T>, readOnly: Boolean = false) {
    if (readOnly) objectProperty.bind(objectProperty1) else objectProperty.bindBidirectional(objectProperty1)
    Bindings.objectBindings.getOrPut(objectProperty, { mutableListOf(objectProperty1) })
}

class CheckComboBoxBinding<T>(
        val checkComboBox: CheckComboBox<T>,
        val listProperty: ListProperty<T>,
        val readOnly: Boolean,
        onChange: (ListChangeListener.Change<out T>) -> Unit = {}
) {

    private val toListener: ListChangeListener<T> = ListChangeListener { change ->
        synchronized(this) {
            listProperty.removeListener(fromListener)
            listProperty.setAll(change.list)
            onChange(change)
            listProperty.addListener(fromListener)
        }
    }

    private val fromListener: ListChangeListener<T> = ListChangeListener { change ->
        synchronized(this) {
            checkComboBox.checkModel.apply {
                checkedItems.removeListener(toListener)
                checkAll(change.list)
                onChange(change)
                checkedItems.addListener(toListener)
            }
        }
    }

    init {
        checkComboBox.checkModel.checkAll(listProperty)
        checkComboBox.checkModel.checkedItems.addListener(toListener)
        if (!readOnly) listProperty.addListener(fromListener)
    }

    fun unbind() {
        synchronized(this) {
            checkComboBox.checkModel.checkedItems.removeListener(toListener)
            listProperty.removeListener(fromListener)
        }
    }
}



