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

import com.waicool20.util.SikuliXLoader
import com.waicool20.util.logging.LoggingEventBus
import com.waicool20.util.logging.loggerFor
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.util.Duration
import tornadofx.*
import java.nio.file.Paths
import kotlin.concurrent.thread


class LoaderView : View() {
    override val root: AnchorPane by fxml("/views/loader.fxml")
    private val statusLabel: Label by fxid()

    private val logger = loggerFor<LoaderView>()

    init {
        title = "WAI2K - Startup"
        statusLabel.text = ""
    }

    override fun onDock() {
        super.onDock()
        startStatusListener()
        logger.info("Starting WAI2K")
        find<ConsoleView>()
        thread { startLoading() }
    }

    private fun startStatusListener() {
        LoggingEventBus.initialize()
        LoggingEventBus.subscribe(Regex(".* - (.*)")) {
            runLater { statusLabel.text = it.groupValues[1] }
        }
    }

    private fun startLoading() {
        SikuliXLoader.loadAndTest(Paths.get("/home/waicool20/bin/sikulix/sikulix.jar"))
        closeAndShowMainApp()
    }

    private fun closeAndShowMainApp() {
        runLater(Duration.millis(500.0)) {
            close()
            primaryStage.show()
            find<ConsoleView>().openWindow(owner = primaryStage)
        }
    }
}
