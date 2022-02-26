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
import com.waicool20.wai2k.game.GFL
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.GameState
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.modules.InitModule
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.script.modules.StopModule
import com.waicool20.wai2k.util.YuuBot
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.reflections.Reflections
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.coroutines.coroutineContext
import kotlin.io.path.createDirectories
import kotlin.math.roundToLong
import kotlin.reflect.full.primaryConstructor

class ScriptRunner(
    wai2KConfig: Wai2KConfig = Wai2KConfig(),
    wai2KProfile: Wai2KProfile = Wai2KProfile()
) {
    companion object {
        const val NORMAL_RES = 480
        const val HIGH_RES = 1080
    }

    enum class State {
        RUNNING, PAUSED, STOPPED
    }

    init {
        // Turn off logging for reflections library
        (loggerFor<Reflections>() as Logger).level = Level.OFF
    }

    lateinit var scope: CoroutineScope
        private set

    private val logger = loggerFor<ScriptRunner>()
    private var currentDevice: AndroidDevice? = null
    private var currentConfig = wai2KConfig
    private var currentProfile = wai2KProfile

    var config: Wai2KConfig = currentConfig
    var profile: Wai2KProfile = currentProfile

    val gameState = GameState()
    val scriptStats = ScriptStats()
    var justRestarted = false
        private set
    var lastStartTime: Instant? = null
        private set
    private val _state = MutableStateFlow(State.STOPPED)
    var state = _state.asStateFlow()

    private var statsHash: Int = scriptStats.hashCode()
    private val modules = mutableSetOf<ScriptModule>()
    private var navigator: Navigator? = null

    fun run() {
        if (!_state.compareAndSet(State.STOPPED, State.RUNNING)) return
        scope = CoroutineScope(Dispatchers.Default + CoroutineName("ScriptRunner"))
        scope.coroutineContext.job.invokeOnCompletion { _state.update { State.STOPPED } }
        logger.info("Starting new WAI2K session")
        gameState.requiresUpdate = true
        lastStartTime = Instant.now()
        scriptStats.reset()
        statsHash = scriptStats.hashCode()
        gameState.reset()
        justRestarted = true
        reload(true)
        scope.launch {
            postStats()
            while (isActive) {
                try {
                    runScriptCycle()
                } catch (e: ScriptException) {
                    e.printStackTrace()
                    logger.warn("Recoverable fault detected, restarting game")
                    saveDebugImage()
                    exceptionRestart(e)
                } catch (e: UnrecoverableScriptException) {
                    e.message?.lines()?.forEach { logger.error(it) }
                    stopNow()
                } catch (e: AndroidDevice.UnexpectedDisconnectException) {
                    logger.error("Emulator disconnected unexpectedly")
                    YuuBot.postMessage(
                        currentConfig.apiKey,
                        "Script Terminated",
                        "Reason: Emulator disconnected"
                    )
                    stopNow()
                } catch (e: Region.CaptureIOException) {
                    if (currentDevice?.isConnected() == true) {
                        logger.error("Screen capture error, will wait 10s before restarting")
                        delay(10_000)
                        exceptionRestart(e)
                    } else {
                        logger.error("Device no longer connected on ADB! Exiting...")
                        YuuBot.postMessage(
                            currentConfig.apiKey,
                            "Script Stopped",
                            "Device is dead!"
                        )
                        stopNow()
                    }
                } catch (e: Exception) {
                    when (e) {
                        is CancellationException -> Unit // Do nothing
                        else -> {
                            val msg =
                                "Uncaught error during script execution, please report this to the devs"
                            logger.error(msg)
                            YuuBot.postMessage(currentConfig.apiKey, "Script Stopped", msg)
                            throw e
                        }
                    }
                }
            }
        }
    }

    fun reload(forceReload: Boolean = false) {
        var reloadModules = forceReload
        if (currentConfig != config) {
            currentConfig = config
            reloadModules = true
        }
        if (currentProfile != profile) {
            currentProfile = profile
            reloadModules = true
        }
        currentConfig.scriptConfig.apply {
            Region.DEFAULT_MATCHER.settings.matchDimension = NORMAL_RES
            Region.DEFAULT_MATCHER.settings.defaultThreshold = defaultSimilarityThreshold
            currentDevice?.input?.touchInterface?.settings?.postTapDelay =
                (mouseDelay * 1000).roundToLong()
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
            GameLocation.mappings(currentConfig, refresh = true)
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
            modules.map { it::class.simpleName }
                .forEach { logger.info("Loaded new instance of $it") }
        }
    }

    fun pause() {
        _state.compareAndSet(State.RUNNING, State.PAUSED)
    }

    fun unpause() {
        _state.compareAndSet(State.PAUSED, State.RUNNING)
    }

    fun stop() {
        logger.info("Stopping the script")
        scope.cancel()
    }

    fun stopNow() {
        stop()
        runBlocking(scope.coroutineContext) { yield() }
    }

    private suspend fun runScriptCycle() {
        reload()
        if (modules.isEmpty()) stopNow()
        modules.forEach { it.execute() }
        justRestarted = false

        postStats()
        if (state.value == State.PAUSED) {
            logger.info("Script is now paused")
            state.first { it == State.RUNNING }
            logger.info("Script will now resume")
        } else {
            delay((currentConfig.scriptConfig.loopDelay * 1000L).coerceAtLeast(500))
        }
    }

    private fun saveDebugImage() {
        val now = LocalDateTime.now()
        val device = requireNotNull(currentDevice)
        val screenshot = device.screens.first().capture()
        val output = Wai2K.CONFIG_DIR.resolve("debug")
            .resolve("${DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss").format(now)}.png")
        scope.launch(Dispatchers.IO) {
            output.parent.createDirectories()
            ImageIO.write(screenshot, "PNG", output.toFile())
            logger.info("Saved debug image to: ${output.toAbsolutePath()}")
        }
    }

    private suspend fun exceptionRestart(e: Exception) {
        if (currentConfig.gameRestartConfig.enabled) {
            restartGame(e.localizedMessage)
        } else {
            if (currentConfig.notificationsConfig.onRestart) {
                YuuBot.postMessage(
                    currentConfig.apiKey,
                    "Script Stopped",
                    "Reason: ${e.localizedMessage}"
                )
            }
            logger.warn("Restart not enabled, ending script here")
            stopNow()
        }
    }

    /**
     * Restarts the game
     * This assumes that automatic login is enabled and no updates are required
     */
    suspend fun restartGame(reason: String) {
        if (scriptStats.gameRestarts >= currentConfig.gameRestartConfig.maxRestarts) {
            logger.info("Maximum of restarts reached, terminating script instead")
            YuuBot.postMessage(currentConfig.apiKey, "Script Terminated", "Max restarts reached")
            stopNow()
        }
        val device = requireNotNull(currentDevice)
        val region = device.screens.first()
        gameState.requiresRestart = false
        scriptStats.gameRestarts++
        if (currentConfig.notificationsConfig.onRestart) {
            YuuBot.postMessage(currentConfig.apiKey, "Script Restarted", "Reason: $reason")
        }
        logger.info("Game will now restart")
        ProcessManager(device).restart(GFL.PKG_NAME)
        logger.info("Game restarted, waiting for login screen")
        region.subRegion(550, 960, 250, 93)
            .waitHas(FileTemplate("login.png", 0.8), 5 * 60 * 1000L)
            ?: logger.warn("Timed out on login!")
        logger.info("Logging in")
        region.subRegion(630, 400, 900, 300).click()
        val locations = GameLocation.mappings(currentConfig)
        val login = region.subRegion(200, 19, 96, 87)
        while (coroutineContext.isActive) {
            navigator?.checkLogistics()
            // Check for sign in or achievement popup
            if (region.subRegion(396, 244, 80, 80).has(FileTemplate("home-popup.png"))) {
                logger.info("Detected popup, dismissing...")
                repeat(2) { region.subRegion(2017, 151, 129, 733).click() }
            }
            // Check for daily login
            if (login.has(FileTemplate("home-popup1.png"))) {
                logger.info("Detected daily login/event screen, dismissing...")
                login.click()
            }
            region.subRegion(900, 720, 350, 185)
                .findBest(FileTemplate("close.png"))?.region?.click()
            if (locations.getValue(LocationId.HOME).isInRegion(region)) {
                logger.info("Logged in, waiting for 10s to see if anything happens")
                delay(10_000)
                if (locations.getValue(LocationId.HOME).isInRegion(region)) {
                    gameState.currentGameLocation = locations.getValue(LocationId.HOME)
                    break
                }
            }
            delay(1000)
        }
        logger.info("Finished logging in")
        justRestarted = true
        gameState.requiresUpdate = true
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