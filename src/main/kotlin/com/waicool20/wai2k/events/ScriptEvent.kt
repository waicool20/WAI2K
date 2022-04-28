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

package com.waicool20.wai2k.events

import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.script.ScriptStats
import java.time.Instant

sealed class ScriptEvent(
    val sessionId: Long,
    val elapsedTime: Long,
    instant: Instant = Instant.now()
) : EventBus.Event(instant)

class HeartBeatEvent(
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class ScriptStatsUpdateEvent(
    val stats: ScriptStats,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class LogisticsSupportReceivedEvent(
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class LogisticsSupportSentEvent(
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class SortieDoneEvent(
    val map: String,
    val draggers: List<Wai2kProfile.DollCriteria>,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class DollDropEvent(
    val doll: String,
    val map: String,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class DollEnhancementEvent(
    val count: Int,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class DollDisassemblyEvent(
    val count: Int,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class EquipDisassemblyEvent(
    val count: Int,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class RepairEvent(
    val count: Int,
    val map: String,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class GameRestartEvent(
    val reason: String,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class CombatReportWriteEvent(
    val type: Wai2kProfile.CombatReport.Type,
    val count: Int,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class SimEnergySpentEvent(
    val type: String,
    val level: Wai2kProfile.CombatSimulation.Level,
    val count: Int,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class CoalitionEnergySpentEvent(
    val type: Wai2kProfile.CombatSimulation.Coalition.Type,
    val count: Int,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

sealed class ScriptStateEvent(
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptEvent(sessionId, elapsedTime, instant)

class ScriptStartEvent(
    val profileName: String,
    sessionId: Long,
    instant: Instant = Instant.now()
) : ScriptStateEvent(sessionId, 0, instant)

class ScriptPauseEvent(
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptStateEvent(sessionId, elapsedTime, instant)

class ScriptUnpauseEvent(
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptStateEvent(sessionId, elapsedTime, instant)

class ScriptStopEvent(
    val reason: String,
    sessionId: Long,
    elapsedTime: Long,
    instant: Instant = Instant.now()
) : ScriptStateEvent(sessionId, elapsedTime, instant)

