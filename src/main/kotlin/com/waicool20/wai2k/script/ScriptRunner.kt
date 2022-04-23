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
import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.events.*
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.GameState
import com.waicool20.wai2k.script.modules.InitModule
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.script.modules.StopModule
import com.waicool20.wai2k.util.YuuBot
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.reflections.Reflections
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.concurrent.fixedRateTimer
import kotlin.io.path.createDirectories
import kotlin.math.roundToLong
import kotlin.reflect.full.primaryConstructor

class ScriptRunner(
    var config: Wai2kConfig = Wai2kConfig(),
    var profile: Wai2kProfile = Wai2kProfile()
) {
    companion object {
        const val NORMAL_RES = 480
        const val HIGH_RES = 1080
    }

    enum class State {
        RUNNING, PAUSING, PAUSED, STOPPED
    }

    lateinit var sessionScope: CoroutineScope
        private set

    private val logger = loggerFor<ScriptRunner>()
    private var _device: AndroidDevice? = null
    private var _config = config
    private var _profile = profile

    val gameState = GameState()
    val scriptStats = ScriptStats()
    var lastStartTime: Instant? = null
        private set
    var elapsedTime = 0L
        private set

    private val _state = MutableStateFlow(State.STOPPED)
    val state get() = _state.value

    private var statsChanged = false
    private val modules = mutableSetOf<ScriptModule>()
    private lateinit var navigator: Navigator

    init {
        // Turn off logging for reflections library
        (loggerFor<Reflections>() as Logger).level = Level.OFF
        fixedRateTimer("ElapsedTimeTimer", true, 0, 1000) {
            if (state == State.RUNNING || state == State.PAUSING) elapsedTime += 1000
        }
    }

    fun run() {
        if (!_state.compareAndSet(State.STOPPED, State.RUNNING)) return
        EventBus.tryPublish(ScriptStartEvent())
        sessionScope = CoroutineScope(Dispatchers.Default + CoroutineName("ScriptRunner"))
        sessionScope.coroutineContext.job.invokeOnCompletion {
            _state.update { State.STOPPED }
            EventBus.tryPublish(ScriptStopEvent())
        }
        logger.info("Starting new WAI2K session")
        gameState.requiresUpdate = true
        elapsedTime = 0
        lastStartTime = Instant.now()
        scriptStats.reset()
        gameState.reset()
        reload(true)
        EventBus.subscribe<ScriptStatsUpdateEvent>()
            .onEach { statsChanged = true }
            .launchIn(sessionScope)
        sessionScope.launch {
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
                    stop()
                } catch (e: AndroidDevice.UnexpectedDisconnectException) {
                    handleDeadDevice()
                } catch (e: Region.CaptureIOException) {
                    if (_device?.isConnected() == true) {
                        logger.error("Screen capture error, will wait 10s before restarting")
                        delay(10_000)
                        exceptionRestart(e)
                    } else {
                        logger.error("Device no longer connected on ADB! Exiting...")
                        YuuBot.postMessage(
                            _config.apiKey,
                            "Script Stopped",
                            "Device is dead!"
                        )
                        stop()
                    }
                } catch (e: Exception) {
                    when (e) {
                        is CancellationException -> Unit // Do nothing
                        else -> {
                            val msg =
                                "Uncaught error during script execution, please report this to the devs"
                            logger.error(msg)
                            YuuBot.postMessage(_config.apiKey, "Script Stopped", msg)
                            throw e
                        }
                    }
                }
            }
        }
    }

    fun reload(forceReload: Boolean = false) {
        var reloadModules = forceReload
        if (_config != config) {
            logger.info("Detected configuration change")
            _config = config
            reloadModules = true
        }
        if (_profile != profile) {
            logger.info("Detected profile change")
            _profile = profile
            reloadModules = true
        }
        _config.scriptConfig.apply {
            Region.DEFAULT_MATCHER.settings.matchDimension = NORMAL_RES
            Region.DEFAULT_MATCHER.settings.defaultThreshold = defaultSimilarityThreshold
            _device?.input?.touchInterface?.settings?.postTapDelay =
                (mouseDelay * 1000).roundToLong()
        }

        if (_device == null || _device?.serial != _config.lastDeviceSerial) {
            _device = ADB.getDevice(_config.lastDeviceSerial)
        }
        val region = _device?.screens?.firstOrNull() ?: run {
            logger.info("Could not start due to invalid device")
            return
        }
        if (reloadModules) {
            logger.info("Reloading modules")
            modules.clear()
            GameLocation.mappings(_config, refresh = true)
            navigator = Navigator(this, region, _config, _profile)
            modules.add(InitModule(navigator))
            Reflections("com.waicool20.wai2k.script.modules")
                .getSubTypesOf(ScriptModule::class.java)
                .map { it.kotlin }
                .filterNot { it.isAbstract || it == InitModule::class || it == StopModule::class }
                .mapNotNull { it.primaryConstructor?.call(navigator) }
                .let { modules.addAll(it) }
            modules.add(StopModule(navigator))
            modules.map { it::class.simpleName }
                .forEach { logger.info("Loaded new instance of $it") }
        }
    }

    fun pause() {
        _state.compareAndSet(State.RUNNING, State.PAUSING)
    }

    fun unpause() {
        _state.compareAndSet(State.PAUSING, State.RUNNING)
        _state.compareAndSet(State.PAUSED, State.RUNNING)
    }

    fun stop() {
        logger.info("Stopping the script")
        sessionScope.cancel()
        try {
            runBlocking(sessionScope.coroutineContext) { yield() }
        } catch (e: CancellationException) {
            // Ignore
        }
    }

    private suspend fun runScriptCycle() {
        reload()
        modules.forEach { it.execute() }

        postStats()
        if (_state.compareAndSet(State.PAUSING, State.PAUSED)) {
            logger.info("Script is now paused")
            EventBus.publish(ScriptPauseEvent())
            _state.first { it == State.RUNNING }
            EventBus.publish((ScriptUnpauseEvent()))
            logger.info("Script will now resume")
        } else {
            delay((_config.scriptConfig.loopDelay * 1000L).coerceAtLeast(500))
        }
    }

    private fun saveDebugImage() {
        val now = LocalDateTime.now()
        val device = requireNotNull(_device)
        val screenshot = device.screens.first().capture()
        val output = Wai2k.CONFIG_DIR.resolve("debug")
            .resolve("${DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss").format(now)}.png")
        sessionScope.launch(Dispatchers.IO) {
            output.parent.createDirectories()
            ImageIO.write(screenshot, "PNG", output.toFile())
            logger.info("Saved debug image to: ${output.toAbsolutePath()}")
        }
    }

    private suspend fun exceptionRestart(e: Exception) {
        try {
            if (_config.gameRestartConfig.enabled) {
                navigator.restartGame(e.localizedMessage)
            } else {
                if (_config.notificationsConfig.onRestart) {
                    YuuBot.postMessage(
                        _config.apiKey,
                        "Script Stopped",
                        "Reason: ${e.localizedMessage}"
                    )
                }
                logger.warn("Restart not enabled, ending script here")
                stop()
            }
        } catch (e: AndroidDevice.UnexpectedDisconnectException) {
            handleDeadDevice()
        }
    }

    private fun postStats() {
        if (statsChanged) {
            statsChanged = false
            val startTime = lastStartTime ?: return
            YuuBot.postStats(_config.apiKey, startTime, _profile, scriptStats)
        }
    }

    private fun handleDeadDevice() {
        logger.error("Emulator disconnected unexpectedly")
        YuuBot.postMessage(
            _config.apiKey,
            "Script Terminated",
            "Reason: Emulator disconnected"
        )
        stop()
    }
}
