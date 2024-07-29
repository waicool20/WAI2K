/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.config.Wai2kPersist
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.game.location.GameLocation
import com.waicool20.wai2k.util.Ocr
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

interface ScriptComponent {
    val scriptRunner: ScriptRunner
    val region: AndroidRegion
    val config: Wai2kConfig
    val profile: Wai2kProfile
    val persist: Wai2kPersist

    val ocr get() = Ocr.forConfig(config)
    val locations get() = GameLocation.mappings(config)
    val scope get() = scriptRunner.sessionScope
    val sessionId get() = scriptRunner.sessionId
    val elapsedTime get() = scriptRunner.elapsedTime


    /**
     * Searches the ADB logs for a certain string, returns false if the op times out or
     * does not find the regex in the logs
     */
    suspend fun waitForLog(
        str: String,
        timeout: Long = Long.MAX_VALUE,
        fn: suspend () -> Unit = {}
    ): Boolean {
        val job = scriptRunner.sessionScope.launch {
            delay(250)
            while (coroutineContext.isActive) fn()
        }
        try {
            withTimeout(timeout) {
                scriptRunner.logcatListener!!.lines.first { it.contains(str) }
            }
            return true
        } catch (e: TimeoutCancellationException) {
            return false
        } finally {
            job.cancel()
        }
    }

    /**
     * Searches the ADB logs for a certain regex string, returns false if the op times out or
     * does not find the regex in the logs
     */
    suspend fun waitForLog(
        regex: Regex,
        timeout: Long = Long.MAX_VALUE,
        fn: suspend () -> Unit = {}
    ): Boolean {
        val job = scriptRunner.sessionScope.launch {
            delay(250)
            while (coroutineContext.isActive) fn()
        }
        try {
            withTimeout(timeout) {
                scriptRunner.logcatListener!!.lines.first { regex.matchEntire(it) != null }
            }
            return true
        } catch (e: TimeoutCancellationException) {
            return false
        } finally {
            job.cancel()
        }
    }
}
