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

import ai.djl.Device
import ai.djl.engine.Engine
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.config.Wai2kPersist
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.events.*
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.waicoolutils.CLib
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.application.Application
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.*

fun main(args: Array<String>) = Wai2k.main(args)

object Wai2k : CliktCommand(treatUnknownOptionsAsArgs = true) {
    init {
        context { allowInterspersedArgs = false }
    }

    private val logger = loggerFor<Wai2k>()
    private val CONFIG_DIR_NAME = "wai2k"

    val VERSION = txtResource("/version.txt") ?: "unknown"

    var CONFIG_DIR = Path("").absolute().resolve(CONFIG_DIR_NAME)
        private set

    val LOG_LEVEL: String? by option("--log-level", help = "Logging level")
        .choice("INFO", "DEBUG")
    val ASSETS_DIR: Path? by option("--assets-dir", help = "Assets directory path")
        .path(canBeFile = false)
    private val EXTRA_ARGS by argument().multiple()

    private var _config: Wai2kConfig? = null
    private var _profile: Wai2kProfile? = null
    private var _persist: Wai2kPersist? = null

    var config: Wai2kConfig
        get() = _config!!
        set(value) {
            EventBus.tryPublish(ConfigUpdateEvent(value))
            _config = value
        }

    var profile: Wai2kProfile
        get() = _profile!!
        set(value) {
            EventBus.tryPublish(ProfileUpdateEvent(value))
            _profile = value
        }

    var persist: Wai2kPersist
        get() = _persist!!
        set(value) {
            EventBus.tryPublish(PersistUpdateEvent(value))
            _persist = value
        }

    lateinit var scriptRunner: ScriptRunner
        private set

    override fun run() {
        thread(name = "Wai2k application loader", isDaemon = true) { initialize() }
        Application.launch(Wai2kUI::class.java)
    }

    private fun initialize() {
        logger.info("Starting WAI2K version $VERSION")
        logger.info("Config directory: $CONFIG_DIR")
        initDirectories()
        initConfig()
        initOpenCV() // Load this first or else it will hang
        initADB()
        initTesseract()
        initML()
        EventBus.tryPublish(StartupCompleteEvent())
        logger.info("\n${txtResource("/banner.txt")}\n\n\tWai2k application startup complete! Welcome back Commander ~\n")
    }

    private fun initConfig() {
        config = Wai2kConfig.load()
        config.debugModeEnabled = when {
            LOG_LEVEL.equals("INFO", true) -> {
                logger.info("Debug logging disabled by command line flag")
                false
            }
            LOG_LEVEL.equals("DEBUG", true) -> {
                logger.info("Debug logging enabled by command line flag")
                false
            }
            else -> {
                logger.info("No logging level in arguments, using config value: ${config.logLevel}")
                config.debugModeEnabled
            }
        }
        if (ASSETS_DIR?.exists() == true) {
            logger.info("Assets directory passed in through command line: $ASSETS_DIR")
            config.assetsDirectory = ASSETS_DIR
        }
        FileTemplate.checkPaths.add(config.assetsDirectory)
        profile = Wai2kProfile.load(config.currentProfile)
        persist = Wai2kPersist.load()
        scriptRunner = ScriptRunner(config, profile, persist)
    }

    private fun initDirectories() {
        val jarPath = Wai2kUI::class.java.protectionDomain.codeSource.location.toURI().toPath()
        if (isRunningJar()) CONFIG_DIR = jarPath.resolveSibling(CONFIG_DIR_NAME)
        CONFIG_DIR.createDirectories()
    }

    private fun initADB() {
        logger.info("Initializing ADB")
        val devices = ADB.getDevices()
        logger.info("Found ${devices.size} devices")
    }

    private fun initTesseract() {
        logger.info("Initializing Tesseract")
        // Try and set the locale for to C for tesseract 4.0 +
        try {
            CLib.Locale.setLocale(CLib.Locale.LC_ALL, "C")
            logger.info("Set LC_ALL=C")
        } catch (t: Throwable) {
            logger.warn("Could not set locale to C, application may crash if using tesseract 4.0+")
        }
    }

    private fun initML() {
        logger.info("Initializing ML")
        // Set inference thread count, anything above 8 seems to be ignored
        val cores = Runtime.getRuntime().availableProcessors().coerceAtMost(8)
        CLib.setEnv("OMP_NUM_THREADS", cores.toString(), true)
        logger.info("Set inference thread count to $cores")

        logger.info("Loading detection model...")
        val gpus = Engine.getInstance().gpuCount
        val device = if (gpus > 0) {
            logger.info("Detected GPUs: $gpus")
            Device.gpu()
        } else {
            logger.warn("No GPU detected, make sure you have CUDA 11.7 installed, using CPU")
            logger.warn("Some operations may run slower")
            Device.cpu()
        }
        Engine.getInstance().newModel("Loading", device).close()
    }

    private fun initOpenCV() {
        logger.info("Initializing OpenCV")
        System.setProperty("org.bytedeco.javacpp.logger.debug", "true")
        Loader.load(opencv_java::class.java)
    }

    private fun txtResource(path: String): String? {
        return Wai2k::class.java.getResourceAsStream(path)?.bufferedReader()?.readText()
    }

    private fun isRunningJar(): Boolean {
        return "${Wai2kUI::class.java.getResource(Wai2kUI::class.simpleName + ".class")}"
            .startsWith("jar")
    }
}
