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

package com.waicool20.wai2k.script

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.Wai2K
import com.waicool20.wai2k.android.ProcessManager
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.*
import com.waicool20.wai2k.script.modules.InitModule
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.script.modules.StopModule
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.YuuBot
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.distanceTo
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import org.reflections.Reflections
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong
import kotlin.reflect.full.primaryConstructor

class ScriptRunner(
    wai2KConfig: Wai2KConfig = Wai2KConfig(),
    wai2KProfile: Wai2KProfile = Wai2KProfile()
) : CoroutineScope {
    companion object {
        const val NORMAL_RES = 480
        const val HIGH_RES = 1080
    }

    private var scriptJob: Job? = null
    override val coroutineContext: CoroutineContext
        get() = scriptJob?.takeIf { it.isActive }?.let { it + Dispatchers.Default }
            ?: Dispatchers.Default

    private val logger = loggerFor<ScriptRunner>()
    private var currentDevice: AndroidDevice? = null
    private var currentConfig = wai2KConfig
    private var currentProfile = wai2KProfile

    var config: Wai2KConfig? = null
    var profile: Wai2KProfile? = null

    var isPaused: Boolean = false
    val isRunning get() = scriptJob?.isActive == true
    val gameState = GameState()
    val scriptStats = ScriptStats()
    var justRestarted = false
        private set
    var lastStartTime: Instant? = null
        private set

    private var statsHash: Int = scriptStats.hashCode()

    init {
        // Turn off logging for reflections library
        (loggerFor<Reflections>() as Logger).level = Level.OFF
    }

    private val modules = mutableSetOf<ScriptModule>()
    private var navigator: Navigator? = null

    fun run() {
        if (isRunning) return
        logger.info("Starting new WAI2K session")
        isPaused = false
        gameState.requiresUpdate = true
        lastStartTime = Instant.now()
        scriptStats.reset()
        statsHash = scriptStats.hashCode()
        gameState.reset()
        justRestarted = true
        scriptJob = launch {
            reload(true)
            postStats()
            while (isActive) {
                runScriptCycle()
            }
        }
    }

    fun reload(forceReload: Boolean = false) {
        var reloadModules = forceReload
        config?.let {
            if (currentConfig != it) {
                currentConfig = it
                reloadModules = true
            }
        }
        profile?.let {
            if (currentProfile != it) {
                currentProfile = it
                reloadModules = true
            }
        }
        currentConfig.scriptConfig.apply {
            Region.DEFAULT_MATCHER.settings.matchDimension = NORMAL_RES
            Region.DEFAULT_MATCHER.settings.defaultThreshold = defaultSimilarityThreshold
            currentDevice?.input?.touchInterface?.settings?.postTapDelay = (mouseDelay * 1000).roundToLong()
        }

        if (currentDevice == null || currentDevice?.serial != currentConfig.lastDeviceSerial) {
            currentDevice = ADB.getDevices().find { it.serial == currentConfig.lastDeviceSerial }
        }
        val region = currentDevice?.screens?.firstOrNull() ?: run {
            logger.info("Could not start due to invalid device")
            return
        }
        if (reloadModules) {
            logger.info("Reloading modules")
            modules.clear()
            val nav = Navigator(this, region, currentConfig, currentProfile)
            navigator = nav
            modules.add(InitModule(nav))
            Reflections("com.waicool20.wai2k.script.modules")
                .getSubTypesOf(ScriptModule::class.java)
                .map { it.kotlin }
                .filterNot { it.isAbstract || it == InitModule::class || it == StopModule::class }
                .mapNotNull { it.primaryConstructor?.call(nav) }
                .let { modules.addAll(it) }
            modules.add(StopModule(nav))
            modules.map { it::class.simpleName }.forEach { logger.info("Loaded new instance of $it") }
        }
    }

    fun join() = runBlocking {
        scriptJob?.join()
    }

    fun stop() {
        logger.info("Stopping the script")
        isPaused = false
        scriptJob?.cancel()
    }

    private suspend fun runScriptCycle() = coroutineScope {
        reload()
        if (modules.isEmpty()) coroutineContext.cancelAndYield()
        try {
            modules.forEach { it.execute() }
            justRestarted = false
        } catch (e: ScriptException) {
            logger.warn("Fault detected, restarting game")
            val now = LocalDateTime.now()
            e.printStackTrace()
            val device = requireNotNull(currentDevice)
            val screenshot = device.screens.first().capture()
            val output = Wai2K.CONFIG_DIR.resolve("debug")
                .resolve("${DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss").format(now)}.png")
            launch(Dispatchers.IO) {
                Files.createDirectories(output.parent)
                ImageIO.write(screenshot, "PNG", output.toFile())
                logger.info("Saved debug image to: ${output.toAbsolutePath()}")
            }
            if (currentConfig.gameRestartConfig.enabled) {
                restartGame(e.localizedMessage)
            } else {
                logger.warn("Restart not enabled, ending script here")
                coroutineContext.cancelAndYield()
            }
        }
        postStats()
        if (isPaused) {
            logger.info("Script is now paused")
            while (isPaused) delay(currentConfig.scriptConfig.loopDelay * 1000L)
            logger.info("Script will now resume")
        } else {
            delay((currentConfig.scriptConfig.loopDelay * 1000L).coerceAtLeast(500))
        }
    }

    /**
     * Restarts the game
     * This assumes that automatic login is enabled and no updates are required
     */
    suspend fun restartGame(reason: String) {
        if (scriptStats.gameRestarts >= currentConfig.gameRestartConfig.maxRestarts) {
            logger.info("Maximum of restarts reached, terminating script instead")
            coroutineContext.cancelAndYield()
        }
        val device = requireNotNull(currentDevice)
        val region = device.screens.first()
        gameState.requiresRestart = false
        scriptStats.gameRestarts++
        if (currentConfig.notificationsConfig.onRestart) {
            YuuBot.postMessage(currentConfig.apiKey, "Script Restarted", "Reason: $reason")
        }
        logger.info("Game will now restart")
        ProcessManager(device).apply {
            kill(GFL.pkgName)
            delay(200)
            start(GFL.pkgName, GFL.mainActivity)
        }
        logger.info("Game restarted, waiting for login screen")
        region.subRegion(550, 960, 250, 93)
            .waitHas(FileTemplate("login.png", 0.8), 5 * 60 * 1000)
            ?: logger.warn("Timed out on login!")
        logger.info("Logging in")
        region.subRegion(630, 400, 900, 300).click()
        val locations = GameLocation.mappings(currentConfig)
        loop@ while (isActive) {
            navigator?.checkLogistics()
            // Check for sign in or achievement popup
            if (region.subRegion(396, 244, 80, 80).has(FileTemplate("home-popup.png"))) {
                repeat(2) { region.subRegion(2017, 151, 129, 733).click() }
            }
            region.subRegion(900, 720, 350, 185).findBest(FileTemplate("close.png"))?.region?.click()
            val r = region.subRegion(1800, 780, 170, 75)
            when {
                Ocr.forConfig(currentConfig).doOCRAndTrim(r).distanceTo("RESUME") <= 3 -> {
                    logger.info("Detected ongoing battle, terminating it first")
                    while (isActive) {
                        if (Ocr.forConfig(currentConfig).doOCRAndTrim(r).distanceTo("RESUME") <= 3) {
                            r.click()
                        }
                        if (region.has(FileTemplate("combat/battle/terminate.png"))) {
                            break
                        }
                    }
                    // Two terminate button checks, one for when we just entered, the other to wait for
                    // any current battle to end.
                    val mapRunnerRegions = MapRunnerRegions(region)
                    while (isActive) {
                        delay(3000)
                        region.waitHas(FileTemplate("combat/battle/terminate.png"), 30000)
                        mapRunnerRegions.terminateMenu.click(); delay(700)
                        mapRunnerRegions.terminate.click(); delay(5000)
                        val locs = arrayOf(LocationId.COMBAT_MENU, LocationId.CAMPAIGN, LocationId.EVENT)
                        for (l in locs) {
                            if (locations[l]!!.isInRegion(region)) {
                                logger.info("Terminated the battle")
                                gameState.currentGameLocation = locations[l]!!
                                break@loop
                            }
                        }
                    }
                }
                locations[LocationId.HOME]?.isInRegion(region) == true -> {
                    gameState.currentGameLocation = locations[LocationId.HOME]!!
                    break@loop
                }
            }
        }
        logger.info("Finished logging in")
        justRestarted = true
    }

    private fun postStats() {
        // Only post new stats if it has changed
        val newHash = scriptStats.hashCode()
        if (statsHash != newHash) {
            statsHash = newHash
            lastStartTime?.let { startTime ->
                YuuBot.postStats(currentConfig.apiKey, startTime, currentProfile, scriptStats)
            }
        }
    }
}