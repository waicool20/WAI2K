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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.wai2k.script.ScriptComponent
import java.nio.file.Files

/**
 * AbsoluteMapRunner is a base abstract class that implements [nodes] and [findRegion], it simply
 * clicks predefined locations.
 *
 * To implement a AbsoluteMapRunner, only map.json must be present in the maps
 * assets. Where:
 *
 * - map.json is a json file containing [MapNode] definitions relative to the on screen location
 */
abstract class AbsoluteMapRunner(scriptComponent: ScriptComponent) : MapRunner(scriptComponent) {

    override val nodes = run {
        val path = config.assetsDirectory.resolve("$PREFIX/map.json")
        if (Files.exists(path)) {
            jacksonObjectMapper().readValue<List<MapNode>>(path.toFile())
        } else {
            emptyList()
        }
    }

    override suspend fun MapNode.findRegion(): AndroidRegion {
        return region.subRegionAs(x, y, width, height)
    }
}