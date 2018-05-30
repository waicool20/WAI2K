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

package com.waicool20.kaga.util.javafx

import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleSetProperty
import javafx.collections.FXCollections

fun <T> List<T>.toProperty() = SimpleListProperty<T>(FXCollections.observableList(this))
fun <T> Set<T>.toProperty() = SimpleSetProperty<T>(FXCollections.observableSet(this))
fun <K, V> Map<K, V>.toProperty() = SimpleMapProperty<K, V>(FXCollections.observableMap(this))
