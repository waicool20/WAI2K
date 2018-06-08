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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.wai2k.Wai2K
import com.waicool20.wai2k.config.Configurations
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptContext
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.waicoolutils.SikuliXLoader
import com.waicool20.waicoolutils.logging.LoggingEventBus
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.application.Application
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.util.Duration
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.concurrent.thread


class LoaderView : View() {
    override val root: AnchorPane by fxml("/views/loader.fxml")
    private val statusLabel: Label by fxid()
    private val parameters: Application.Parameters by param()

    private val logger = loggerFor<LoaderView>()
    private val configs = Configurations().also { setInScope(it) }
    private var wai2KConfig by configs.wai2KConfigProperty
    private var currentProfile by configs.currentProfileProperty

    init {
        title = "WAI2K - Startup"
        statusLabel.text = ""
    }

    override fun onDock() {
        super.onDock()
        startStatusListener()
        find<ConsoleView>()
        parseVersion()
        logger.info("Starting WAI2K ${configs.versionInfo.version}")
        logger.info("Config directory: ${Wai2K.CONFIG_DIR}")
        startLoading()
    }

    private fun startStatusListener() {
        LoggingEventBus.initialize()
        LoggingEventBus.subscribe(Regex(".* - (.*)")) {
            runLater { statusLabel.text = it.groupValues[1] }
        }
    }

    private fun parseVersion() {
        configs.versionInfo = jacksonObjectMapper().readValue(javaClass.classLoader.getResourceAsStream("version.txt"))
    }

    private fun startLoading() {
        loadWai2KConfig()
        loadWai2KProfile()
        loadScriptRunner()
        thread {
            parseCommandLine()
            loadSikuliX()
            closeAndShowMainApp()
        }
    }

    private fun loadWai2KConfig() {
        wai2KConfig = Wai2KConfig.load()
        if (!wai2KConfig.isValid) {
            find<InitialConfigurationView>().openModal(owner = currentWindow, block = true)
        }
    }

    private fun loadWai2KProfile() {
        currentProfile = Wai2KProfile.load(wai2KConfig.currentProfile)
    }

    private fun loadScriptRunner() {
        setInScope(ScriptContext(ScriptRunner(wai2KConfig, currentProfile)))
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

        parameters.named["assets-dir"]?.let { Paths.get(it) }?.let { dir ->
            logger.info("Assets directory passed in through command line: $dir")
            if (Files.exists(dir)) {
                wai2KConfig.assetsDirectory = dir
            }
        }
    }

    private fun loadSikuliX() {
        SikuliXLoader.loadAndTest(wai2KConfig.sikulixJarPath)
        if (Files.exists(wai2KConfig.assetsDirectory)) {
            SikuliXLoader.loadImagePath(wai2KConfig.assetsDirectory)
            logger.info("Loading assets @ ${wai2KConfig.assetsDirectory}")
        }
    }

    private fun closeAndShowMainApp() {
        logger.info("Loading all done! Starting main application")
        runLater(Duration.millis(500.0)) {
            close()
            primaryStage.show()
            if (wai2KConfig.showConsoleOnStart) {
                find<ConsoleView>().openWindow(owner = null)
            }
        }
    }
}
