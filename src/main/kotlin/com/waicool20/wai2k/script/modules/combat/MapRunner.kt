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

package com.waicool20.wai2k.script.modules.combat

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import kotlinx.coroutines.CoroutineScope
import org.reflections.Reflections
import kotlin.coroutines.CoroutineContext

abstract class MapRunner(
        protected val scriptRunner: ScriptRunner,
        protected val region: AndroidRegion,
        protected val config: Wai2KConfig,
        protected val profile: Wai2KProfile
) : CoroutineScope {

    companion object {
        val list = Reflections("com.waicool20.wai2k.script.modules.combat.maps")
                .getSubTypesOf(MapRunner::class.java)
                .mapNotNull { cls ->
                    Regex("Map(\\d_\\d\\w?)").matchEntire(cls.simpleName)?.let {
                        it.groupValues[1].replace("_", "-") to cls.kotlin
                    }
                }.toMap()
    }

    override val coroutineContext: CoroutineContext
        get() = scriptRunner.coroutineContext

    val gameState get() = scriptRunner.gameState
    val scriptStats get() = scriptRunner.scriptStats

    abstract suspend fun execute()
}