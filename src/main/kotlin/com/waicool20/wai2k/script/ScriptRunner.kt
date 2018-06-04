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

import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import kotlinx.coroutines.experimental.*

object ScriptRunner {
    private val context = newSingleThreadContext("Wai2K Script Runner Context")

    private var currentConfig = Wai2KConfig()
    private var currentProfile = Wai2KProfile()

    var scriptJob: Job? = null

    var config: Wai2KConfig? = null
    var profile: Wai2KProfile? = null

    var isPaused: Boolean = false
    val isRunning get() = scriptJob?.isActive == true

    fun run() {
        if (!isRunning) {
            isPaused = false
            scriptJob = launch(context) {
                (0..1000).forEach {
                    delay(100)
                    println(it)
                    while (isPaused) delay(100)
                }
                reload()
            }
        }
    }

    fun reload() {
        config?.let { currentConfig = it }
        profile?.let { currentProfile = it }
    }

    fun waitFor() = runBlocking {
        scriptJob?.join()
    }

    fun stop() {
        scriptJob?.cancel()
    }
}