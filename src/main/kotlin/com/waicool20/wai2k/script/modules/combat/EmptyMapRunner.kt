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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.wai2k.script.ScriptComponent

/**
 * An empty map runner, [nodes] is an empty list, [begin] does nothing and [findRegion] returns
 * the whole screen. Convenient to use if you only want some functions in [MapRunner] without
 * all the implementation boilerplate
 */
open class EmptyMapRunner(scriptComponent: ScriptComponent) : MapRunner(scriptComponent) {
    override val nodes: List<MapNode>
        get() = emptyList()

    override suspend fun begin() = Unit

    override suspend fun MapNode.findRegion(): AndroidRegion = region
}