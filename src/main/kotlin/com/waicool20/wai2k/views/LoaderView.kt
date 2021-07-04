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

import ai.djl.Device
import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.Wai2K
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptContext
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.ai.ModelLoader
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.logging.LoggingEventBus
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.application.Application
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import tornadofx.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream

class LoaderView : CoroutineScopeView() {
    override val root: AnchorPane by fxml("/views/loader.fxml")
    private val statusLabel: Label by fxid()
    private val parameters: Application.Parameters by param()

    private val logger = loggerFor<LoaderView>()
    private val context = Wai2KContext().also { setInScope(it) }
    private var wai2KConfig by context.wai2KConfigProperty
    private var currentProfile by context.currentProfileProperty

    init {
        title = "WAI2K - Startup"
        statusLabel.text = ""
    }

    override fun onDock() {
        super.onDock()
        launch(Dispatchers.IO) {
            startStatusListener()
            find<ConsoleView>()
            parseVersion()
            logger.info("Starting WAI2K ${context.version}")
            logger.info("Config directory: ${Wai2K.CONFIG_DIR}")
            startLoading()
        }
    }

    private fun startStatusListener() {
        LoggingEventBus.initialize()
        LoggingEventBus.subscribe(Regex(".* - (.*)")) {
            launch { statusLabel.text = it.groupValues[1] }
        }
    }

    private fun parseVersion() {
        context.version = LoaderView::class.java.getResourceAsStream("/version.txt")
            ?.bufferedReader()?.readText()
    }

    private fun startLoading() {
        loadADB()
        loadWai2KConfig()
        loadWai2KProfile()
        loadScriptRunner()
        FileTemplate.checkPaths.add(wai2KConfig.assetsDirectory)
        loadAI()
        closeAndShowMainApp()
    }

    private fun loadADB() {
        logger.info("Preparing ADB...")
        ADB.getDevices()
    }

    private fun loadWai2KConfig() {
        wai2KConfig = Wai2KConfig.load()
        parseCommandLine()
    }

    private fun loadWai2KProfile() {
        currentProfile = Wai2KProfile.load(wai2KConfig.currentProfile)
    }

    private fun loadScriptRunner() {
        setInScope(ScriptContext(ScriptRunner(wai2KConfig, currentProfile)))
    }

    private fun loadAI() {
        logger.info("Loading detection model...")
        val gpus = Device.getGpuCount()
        val device = if (gpus > 0) {
            logger.info("Detected GPUs: $gpus")
            Device.gpu()
        } else {
            logger.warn("No GPU detected, make sure you have CUDA 10.2 or 11.1 installed, using CPU")
            logger.warn("Some operations may run slower")
            Device.cpu()
        }
        ModelLoader.engine.newModel("Loading", device).close()
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
        with(parameters) {
            (named["assets-dir"] ?: named["asset-dir"])?.let { Path(it) }?.let { dir ->
                logger.info("Assets directory passed in through command line: $dir")
                if (dir.exists()) {
                    wai2KConfig.assetsDirectory = dir
                }
            }
        }
    }

    private fun closeAndShowMainApp() {
        logger.info("Loading all done! Starting main application")
        launch {
            delay(500)
            close()
            primaryStage.show()
            if (wai2KConfig.showConsoleOnStart) {
                find<ConsoleView>().openWindow(owner = null)
            }
            Wai2KWorkspace.setDarkMode(wai2KConfig.appearanceConfig.darkMode)
        }
    }
}
