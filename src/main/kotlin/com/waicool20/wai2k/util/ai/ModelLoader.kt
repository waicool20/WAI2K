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

package com.waicool20.wai2k.util.ai

import ai.djl.Device
import ai.djl.Model
import ai.djl.pytorch.engine.PtEngine
import java.nio.file.Files
import java.nio.file.Path

object ModelLoader {
    val engine by lazy { PtEngine.getInstance() }

    fun loadModel(path: Path): Model {
        require(Files.isRegularFile(path)) { "Must be path to model file" }
        require(Files.exists(path)) { "Model file does not exist" }
        require("$path".endsWith(".pt")) { "Model must have .pt extension" }
        val name = "${path.fileName}".dropLastWhile { it != '.' }.dropLast(1)
        val device = if (Device.getGpuCount() > 0) Device.gpu() else Device.cpu()
        return engine.newModel(name, device).apply { load(path.parent, name) }
    }
}