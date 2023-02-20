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

package com.waicool20.wai2k.util.ai

import ai.djl.Device
import ai.djl.Model
import ai.djl.engine.Engine
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension

object ModelLoader {
    fun loadModel(path: Path): Model {
        require(path.isRegularFile()) { "Must be path to model file" }
        require(path.exists()) { "Model file does not exist" }
        require(path.extension == "pt") { "Model must have .pt extension" }
        val name = path.nameWithoutExtension
        val device = if (Engine.getInstance().gpuCount > 0) Device.gpu() else Device.cpu()
        return Engine.getInstance().newModel(name, device).apply { load(path.parent, name) }
    }
}
