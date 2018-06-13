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
import com.waicool20.wai2k.android.AndroidDevice
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.GameState
import com.waicool20.wai2k.script.modules.InitModule
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.experimental.*
import org.reflections.Reflections
import org.sikuli.basics.Settings
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.coroutineContext
import kotlin.reflect.full.primaryConstructor

class ScriptRunner(wai2KConfig: Wai2KConfig = Wai2KConfig(), wai2KProfile: Wai2KProfile = Wai2KProfile()) {
    private val dispatcher = newSingleThreadContext("Wai2K Script Runner Context")

    private var currentDevice: AndroidDevice? = null
    private var currentConfig = wai2KConfig
    private var currentProfile = wai2KProfile

    var config: Wai2KConfig? = null
    var profile: Wai2KProfile? = null

    var scriptJob: Job? = null

    var isPaused: Boolean = false
    val isRunning get() = scriptJob?.isActive == true
    val gameState = GameState()
    var lastStartTime: Instant? = null
        private set

    init {
        // Turn off logging for reflections library
        (loggerFor<Reflections>() as Logger).level = Level.OFF
    }

    private val modules = mutableSetOf<ScriptModule>()

    fun run() {
        if (isRunning) return
        isPaused = false
        gameState.requiresUpdate = true
        lastStartTime = Instant.now()
        scriptJob = launch(dispatcher) {
            while (isActive) {
                runScriptCycle()
            }
        }
    }

    fun reload() {
        var reloadModules = false
        config?.let {
            currentConfig = it
            reloadModules = true
        }
        profile?.let {
            currentProfile = it
            reloadModules = true
        }
        currentConfig.scriptConfig.apply {
            Settings.MinSimilarity = defaultSimilaryThreshold
            Settings.WaitScanRate = sikulixScanRate.toFloat()
            Settings.ObserveScanRate = sikulixScanRate.toFloat()
            Settings.AutoWaitTimeout = 1f
            Settings.RepeatWaitTime = 0
        }

        currentDevice = AndroidDevice.listAll().find { it.adbSerial == currentConfig.lastDeviceSerial }
        if (reloadModules) {
            modules.clear()
            val region = currentDevice?.screen ?: return
            val nav = Navigator(gameState, region, currentConfig, currentProfile)
            modules.add(InitModule(gameState, region, currentConfig, currentProfile, nav))
            Reflections("com.waicool20.wai2k.script.modules")
                    .getSubTypesOf(ScriptModule::class.java)
                    .map { it.kotlin }.filterNot { it.isAbstract }
                    .filterNot { it == InitModule::class }
                    .mapNotNull {
                        it.primaryConstructor?.call(gameState, region, currentConfig, currentProfile, nav)
                    }
                    .let { modules.addAll(it) }
        }
    }

    fun waitFor() = runBlocking {
        scriptJob?.join()
    }

    fun stop() {
        scriptJob?.cancel()
    }

    private suspend fun runScriptCycle() {
        reload()
        if (modules.isEmpty()) coroutineContext.cancelAndYield()
        modules.forEach { it.execute() }
        do {
            delay(currentConfig.scriptConfig.loopDelay.toLong(), TimeUnit.SECONDS)
        } while (isPaused)
    }
}