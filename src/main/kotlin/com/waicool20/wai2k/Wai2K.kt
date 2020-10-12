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

package com.waicool20.wai2k

import com.waicool20.wai2k.views.LoaderView
import com.waicool20.wai2k.views.Wai2KWorkspace
import com.waicool20.waicoolutils.CLib
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.stage.Stage
import javafx.stage.StageStyle
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Wai2K : App(Wai2KWorkspace::class) {
    companion object {
        const val CONFIG_DIR_NAME = "wai2k"
        const val CONFIG_SUFFIX = ".json"
        private val logger = loggerFor<Companion>()
        private var _configDirectory: Path = Paths.get("").toAbsolutePath().resolve(CONFIG_DIR_NAME)
        val CONFIG_DIR get() = _configDirectory

        init {
            val jarPath = Paths.get(Wai2K::class.java.protectionDomain.codeSource.location.toURI())
            if (isRunningJar()) _configDirectory = jarPath.resolveSibling(CONFIG_DIR_NAME)
            if (Files.notExists(_configDirectory)) Files.createDirectories(_configDirectory)
            // Try and set the locale for to C for tesseract 4.0 +
            try {
                CLib.Locale.setLocale(CLib.Locale.LC_ALL, "C")
            } catch (t: Throwable) {
                logger.warn("Could not set locale to C, application may crash if using tesseract 4.0+")
            }
            // Set inference thread count, anything above 8 seems to be ignored
            val cores = Runtime.getRuntime().availableProcessors().coerceAtMost(8)
            CLib.setEnv("OMP_NUM_THREADS", cores.toString(), true)
        }

        private fun isRunningJar(): Boolean {
            return "${Wai2K::class.java.getResource(Wai2K::class.simpleName + ".class")}".startsWith("jar")
        }
    }

    override fun start(stage: Stage) {
        super.start(stage)
        find<LoaderView>(params = arrayOf("parameters" to parameters))
            .openModal(stageStyle = StageStyle.UNDECORATED)
    }

    override fun shouldShowPrimaryStage() = false
}

