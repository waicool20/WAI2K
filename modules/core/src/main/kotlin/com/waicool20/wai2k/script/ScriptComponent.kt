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
        period: Long = -1,
        timeout: Long = Long.MAX_VALUE,
        fnMaxIter: Int = Int.MAX_VALUE,
        fn: (suspend () -> Unit)? = null
    ): Boolean {
        return waitForLog(Regex(".*$str.*"), period, timeout, fnMaxIter, fn)
    }

    /**
     * Searches the ADB logs for a certain regex string, returns false if the op times out or
     * does not find the regex in the logs
     */
    suspend fun waitForLog(
        regex: Regex,
        period: Long = -1,
        timeout: Long = Long.MAX_VALUE,
        fnMaxIter: Int = Int.MAX_VALUE,
        fn: (suspend () -> Unit)? = null
    ): Boolean {
        val log = scriptRunner.sessionScope.async {
            try {
                withTimeout(timeout) {
                    scriptRunner.logcatListener!!.lines.first { regex.matchEntire(it) != null }
                }
                true
            } catch (e: TimeoutCancellationException) {
                false
            }
        }
        val job = scriptRunner.sessionScope.launch {
            if (fn == null) return@launch
            var i = 0
            while (coroutineContext.isActive && i++ < fnMaxIter) {
                fn()
                if (period > 0) delay(period)
            }
        }
        val b = log.await()
        job.cancel()
        return b
    }
}
