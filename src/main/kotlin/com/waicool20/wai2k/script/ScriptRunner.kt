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

import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.Region
import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.config.Wai2kPersist
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.events.*
import com.waicool20.wai2k.game.GFL
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.GameState
import com.waicool20.wai2k.script.modules.InitModule
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.script.modules.StopModule
import com.waicool20.wai2k.util.YuuBot
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.reflections.Reflections
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.concurrent.fixedRateTimer
import kotlin.io.path.createDirectories
import kotlin.math.roundToLong
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.system.exitProcess

class ScriptRunner(
    var config: Wai2kConfig = Wai2kConfig(),
    var profile: Wai2kProfile = Wai2kProfile(),
    var persist: Wai2kPersist = Wai2kPersist()
) {
    enum class State {
        RUNNING, PAUSING, PAUSED, STOPPED
    }

    private val sessionDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
    lateinit var sessionScope: CoroutineScope
        private set

    private val logger = loggerFor<ScriptRunner>()
    private var _device: AndroidDevice? = null
    private var logcatListener: GFL.LogcatListener? = null
    private var _config = config
    private var _profile = profile

    val gameState = GameState()
    val scriptStats = ScriptStats(this)
    var lastStartTime: Instant? = null
        private set
    val sessionId get() = lastStartTime?.toEpochMilli() ?: -1
    var elapsedTime = 0L
        private set
    val yuubot = YuuBot(config.apiKey)

    private val _state = MutableStateFlow(State.STOPPED)
    val state get() = _state.value

    private var statsChanged = false
    private val modules = mutableSetOf<ScriptModule>()
    private lateinit var navigator: Navigator

    init {
        fixedRateTimer("ElapsedTimeTimer", true, 0, 1000) {
            if (state == State.RUNNING || state == State.PAUSING) {
                elapsedTime += 1000
            }
            if (state != State.STOPPED && elapsedTime % TimeUnit.MINUTES.toMillis(10) == 0L) {
                EventBus.tryPublish(HeartBeatEvent(sessionId, elapsedTime))
            }
        }
    }

    fun run() {
        if (!_state.compareAndSet(State.STOPPED, State.RUNNING)) return
        sessionScope = CoroutineScope(sessionDispatcher + CoroutineName("ScriptRunner"))
        sessionScope.coroutineContext.job.invokeOnCompletion(::onStop)
        listenEvents()
        logger.info("Starting new WAI2K session")
        gameState.requiresUpdate = true
        elapsedTime = 0
        scriptStats.reset()
        gameState.reset()
        val now = Instant.now()
        lastStartTime = now
        EventBus.tryPublish(ScriptStartEvent(profile.name, now.toEpochMilli()))
        sessionScope.launch(CoroutineName("DeviceMonitor") + Dispatchers.IO) {
            while (coroutineContext.isActive) {
                if (_device?.isConnected() == false) stop("Device disconnected")
                delay(TimeUnit.MINUTES.toMillis(1))
            }
        }
        sessionScope.launch(CoroutineName("ScriptRunnerMainLoop")) {
            while (coroutineContext.isActive) {
                try {
                    runScriptCycle()
                } catch (e: ScriptException) {
                    e.printStackTrace()
                    logger.warn("Recoverable fault detected, restarting game")
                    saveDebugImage()
                    exceptionRestart(e)
                } catch (e: UnrecoverableScriptException) {
                    e.message?.lines()?.forEach { logger.error(it) }
                    stop(e::class.simpleName ?: "")
                } catch (e: AndroidDevice.UnexpectedDisconnectException) {
                    handleDeadDevice()
                } catch (e: Region.CaptureIOException) {
                    if (_device?.isConnected() == true) {
                        logger.error("Screen capture error, will wait 10s before restarting")
                        delay(10_000)
                        exceptionRestart(e)
                    } else {
                        logger.error("Device no longer connected on ADB! Exiting...")
                        stop("Device disconnected")
                    }
                } catch (e: Exception) {
                    when (e) {
                        is CancellationException -> Unit // Do nothing
                        else -> {
                            val msg =
                                "Uncaught error during script execution, please report this to the devs"
                            logger.error(msg, e)
                            stop(msg)
                            throw e
                        }
                    }
                }
            }
        }
    }

    fun reload(forceReload: Boolean = false) {
        // Force reload if just started
        var reloadModules = elapsedTime < 2000 || forceReload
        if (_config != config) {
            logger.info("Detected configuration change")
            _config = config
            yuubot.apiKey = _config.apiKey
            reloadModules = true
        }
        if (_profile != profile) {
            logger.info("Detected profile change")
            _profile = profile
            reloadModules = true
        }
        if (_device == null || _device?.serial != _config.lastDeviceSerial) {
            val device =
                ADB.getDevice(_config.lastDeviceSerial) ?: throw InvalidDeviceException(null)
            _device = device
            logcatListener?.stop()
            logcatListener = GFL.LogcatListener(this, device)
        }
        logcatListener?.start()
        _config.scriptConfig.apply {
            Region.DEFAULT_MATCHER.settings.defaultThreshold = defaultSimilarityThreshold
            _device?.input?.touchInterface?.settings?.postTapDelay =
                (mouseDelay * 1000).roundToLong()
        }

        val region = _device?.screens?.firstOrNull() ?: throw InvalidDeviceException(_device)
        if (reloadModules) {
            logger.info("Reloading modules")
            modules.clear()
            GameLocation.mappings(_config, refresh = true)
            navigator = Navigator(this, region, _config, _profile, persist)
            modules.add(InitModule(navigator))
            Reflections("com.waicool20.wai2k.script.modules")
                .getSubTypesOf(ScriptModule::class.java)
                .map { it.kotlin }
                .filterNot { it.isAbstract || it == InitModule::class || it == StopModule::class }
                .filterNot {
                    val a =
                        it.findAnnotation<ScriptModule.DisableModule>() ?: return@filterNot false
                    logger.info("${it.simpleName} is disabled, reason: ${a.reason}")
                    true
                }
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

    fun stop(reason: String = "Manual stop") {
        logger.info("Stopping the script")
        sessionScope.cancel(reason)
        try {
            runBlocking(sessionScope.coroutineContext) { yield() }
        } catch (e: CancellationException) {
            // Ignore
        }
    }

    private suspend fun runScriptCycle() {
        reload()
        modules.forEach { it.execute() }
        if (_state.compareAndSet(State.PAUSING, State.PAUSED)) {
            logger.info("Script is now paused")
            EventBus.publish(ScriptPauseEvent(sessionId, elapsedTime))
            _state.first { it == State.RUNNING }
            EventBus.publish((ScriptUnpauseEvent(sessionId, elapsedTime)))
            logger.info("Script will now resume")
        } else {
            delay(_config.scriptConfig.loopDelay.coerceAtLeast(1) * 1000L)
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
                logger.warn("Restart not enabled, ending script here")
                stop(e.localizedMessage)
            }
        } catch (e: AndroidDevice.UnexpectedDisconnectException) {
            handleDeadDevice()
        }
    }

    private fun handleDeadDevice() {
        logger.error("Device disconnected unexpectedly")
        stop("Device disconnected")
    }

    private fun onStop(ce: Throwable?) {
        logcatListener?.stop()
        _state.update { State.STOPPED }
        ScriptStopEvent("${ce?.message}", sessionId, elapsedTime).let {
            EventBus.tryPublish(it)
            yuubot.postEvent(it)
        }
        val msg = """
            |Reason: ${ce?.message}
            |Terminating further execution, final script statistics: 
            |```
            |$scriptStats
            |```
            """.trimMargin()
        logger.info(msg)
        val latch = CountDownLatch(1)
        if (config.notificationsConfig.onStopCondition) {
            yuubot.postMessage("Script Stopped", msg) { latch.countDown() }
        }
        latch.await()
        if (ce?.message != "Manual stop" && profile.stop.exitProgram) exitProcess(0)
    }

    fun listenEvents() {
        EventBus.subscribe<ScriptStatsUpdateEvent>()
            .onEach { statsChanged = true }
            .launchIn(sessionScope + CoroutineName("ScriptStatsUpdateListener"))
        EventBus.subscribe<ScriptEvent>()
            .onEach { yuubot.postEvent(it) }
            .launchIn(sessionScope + CoroutineName("ScriptEventListener"))
        EventBus.subscribe<GameRestartEvent>()
            .onEach {
                if (config.notificationsConfig.onRestart) {
                    yuubot.postMessage("Game Restarted", "Reason: ${it.reason}")
                }
            }.launchIn(sessionScope + CoroutineName("GameRestartListener"))
    }
}
