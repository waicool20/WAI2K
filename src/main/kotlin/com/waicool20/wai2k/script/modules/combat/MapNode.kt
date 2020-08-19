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

package com.waicool20.wai2k.script.modules.combat

import com.fasterxml.jackson.annotation.JsonInclude
import java.awt.Rectangle

@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class MapNode(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val type: Type = Type.Normal
) {
    enum class Type { Normal, CommandPost, Heliport, HeavyHeliport }

    class RelativeMapNode(x: Int, y: Int, width: Int, height: Int) : MapNode(x, y, width, height) {
        override fun toString() = "RelativeMapNode(x=$x, y=$y, width=$width, height=$height, type=$type)"
    }

    class AbsoluteMapNode(x: Int, y: Int, width: Int, height: Int) : MapNode(x, y, width, height) {
        override fun toString() = "AbsoluteMapNode(x=$x, y=$y, width=$width, height=$height, type=$type)"
    }

    val rect by lazy { Rectangle(x, y, width, height) }
}