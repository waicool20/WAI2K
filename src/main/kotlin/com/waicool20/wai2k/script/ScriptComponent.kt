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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.YuuBot
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.Job
import kotlin.system.exitProcess

interface ScriptComponent {
    val scriptRunner: ScriptRunner
    val region: AndroidRegion
    val config: Wai2kConfig
    val profile: Wai2kProfile

    val ocr get() = Ocr.forConfig(config)
    val locations get() = GameLocation.mappings(config)
    val scope get() = scriptRunner.sessionScope

    suspend fun stopScriptWithReason(reason: String) {
        val msg = """
            |Script stop condition reached: $reason
            |Terminating further execution, final script statistics: 
            |```
            |${scriptRunner.scriptStats}
            |```
            """.trimMargin()
        loggerFor<ScriptComponent>().info(msg)
        val wait = Job()
        if (config.notificationsConfig.onStopCondition) {
            YuuBot.postMessage(config.apiKey, "Script Terminated", msg) { wait.complete() }
        }
        if (profile.stop.exitProgram) {
            wait.join()
            exitProcess(0)
        }
        scriptRunner.stop()
    }
}
