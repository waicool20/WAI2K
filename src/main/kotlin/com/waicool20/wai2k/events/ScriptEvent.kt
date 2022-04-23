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

import com.waicool20.wai2k.script.ScriptStats
import java.time.Instant

sealed class ScriptEvent(instant: Instant = Instant.now()) : EventBus.Event(instant)

class ScriptStatsUpdateEvent(val stats: ScriptStats, instant: Instant = Instant.now()) :
    ScriptEvent(instant)

class LogisticsSupportReceivedEvent(instant: Instant = Instant.now()) : ScriptEvent(instant)
class LogisticsSupportSentEvent(instant: Instant = Instant.now()) : ScriptEvent(instant)
class SortieDoneEvent(instant: Instant = Instant.now()) : ScriptEvent(instant)
class DollEnhancementDoneEvent(val count: Int, instant: Instant = Instant.now()) :
    ScriptEvent(instant)

class DollDisassemblyDoneEvent(val count: Int, instant: Instant = Instant.now()) :
    ScriptEvent(instant)

class EquipDisassemblyDoneEvent(val count: Int, instant: Instant = Instant.now()) :
    ScriptEvent(instant)

class RepairsDoneEvent(val count: Int, instant: Instant = Instant.now()) : ScriptEvent(instant)
class GameRestartEvent(val reason: String, instant: Instant = Instant.now()) : ScriptEvent(instant)
class CombatReportWriteEvent(val count: Int, instant: Instant = Instant.now()) :
    ScriptEvent(instant)

class SimEnergySpentEvent(val count: Int, instant: Instant = Instant.now()) : ScriptEvent(instant)
class CoalitionEnergySpentEvent(val count: Int, instant: Instant = Instant.now()) :
    ScriptEvent(instant)

sealed class ScriptStateEvent(instant: Instant = Instant.now()) : ScriptEvent(instant)

class ScriptStartEvent(instant: Instant = Instant.now()) : ScriptStateEvent(instant)
class ScriptPauseEvent(instant: Instant = Instant.now()) : ScriptStateEvent(instant)
class ScriptUnpauseEvent(instant: Instant = Instant.now()) : ScriptStateEvent(instant)
class ScriptStopEvent(instant: Instant = Instant.now()) : ScriptStateEvent(instant)

