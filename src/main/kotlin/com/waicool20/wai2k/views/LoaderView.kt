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

import ch.qos.logback.classic.Level
import com.waicool20.util.SikuliXLoader
import com.waicool20.util.logging.LoggerUtils
import com.waicool20.util.logging.LoggingEventBus
import com.waicool20.util.logging.loggerFor
import com.waicool20.wai2k.Wai2K
import com.waicool20.wai2k.config.Configurations
import com.waicool20.wai2k.config.Wai2KConfig
import javafx.application.Application
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.util.Duration
import tornadofx.*
import kotlin.concurrent.thread


class LoaderView : View() {
    override val root: AnchorPane by fxml("/views/loader.fxml")
    private val statusLabel: Label by fxid()
    private val parameters: Application.Parameters by param()

    private val logger = loggerFor<LoaderView>()
    private lateinit var wai2KConfig: Wai2KConfig

    init {
        title = "WAI2K - Startup"
        statusLabel.text = ""
    }

    override fun onDock() {
        super.onDock()
        startStatusListener()
        find<ConsoleView>()
        logger.info("Starting WAI2K")
        logger.info("Config directory: ${Wai2K.CONFIG_DIR}")
        startLoading()
    }

    private fun startStatusListener() {
        LoggingEventBus.initialize()
        LoggingEventBus.subscribe(Regex(".* - (.*)")) {
            runLater { statusLabel.text = it.groupValues[1] }
        }
    }

    private fun startLoading() {
        loadWai2KConfig()
        thread {
            parseCommandLine()
            loadSikuliX()
            closeAndShowMainApp()
        }
    }

    private fun loadWai2KConfig() {
        wai2KConfig = Wai2KConfig.load()
        setInScope(Configurations(wai2KConfig))
        if (!wai2KConfig.isValid) {
            find<InitialConfigurationView>().openModal(owner = currentWindow, block = true)
        }
    }

    private fun parseCommandLine() {
        val logLevel = parameters.named["log"]
        wai2KConfig.debugModeEnabled = when {
            logLevel.equals("INFO", true) -> {
                logger.info("Debug logging disabled by command line flag")
                false
            }
            logLevel.equals("DEBUG", true) -> {
                logger.info("Debug logging enabled by command line flag")
                false
            }
            else -> {
                logger.info("No logging level in arguments, using config value: ${wai2KConfig.logLevel}")
                wai2KConfig.debugModeEnabled
            }
        }
    }

    private fun loadSikuliX() {
        SikuliXLoader.loadAndTest(wai2KConfig.sikulixJarPath)
    }

    private fun closeAndShowMainApp() {
        logger.info("Loading all done! Starting main application")
        runLater(Duration.millis(500.0)) {
            close()
            primaryStage.show()
            find<ConsoleView>().openWindow(owner = null)
        }
    }
}
