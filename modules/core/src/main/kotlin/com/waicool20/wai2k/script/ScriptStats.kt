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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.waicool20.wai2k.events.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ScriptStats(private val runner: ScriptRunner) {
    private val scope = CoroutineScope(Dispatchers.Default)

    var logisticsSupportReceived: Int = 0
        private set
    var logisticsSupportSent: Int = 0
        private set
    var sortiesDone: Int = 0
        private set
    var enhancementsDone: Int = 0
        private set
    var dollsUsedForEnhancement: Int = 0
        private set
    var disassemblesDone: Int = 0
        private set
    var dollsUsedForDisassembly: Int = 0
        private set
    var equipDisassemblesDone: Int = 0
        private set
    var equipsUsedForDisassembly: Int = 0
        private set
    var repairs: Int = 0
        private set
    var gameRestarts: Int = 0
        private set
    var combatReportsWritten: Int = 0
        private set
    var simEnergySpent: Int = 0
        private set
    var coalitionEnergySpent: Int = 0
        private set

    init {
        EventBus.subscribe<LogisticsSupportReceivedEvent>()
            .onEach {
                logisticsSupportReceived++
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<LogisticsSupportSentEvent>()
            .onEach {
                logisticsSupportSent++
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<SortieDoneEvent>()
            .onEach {
                sortiesDone++
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<DollEnhancementEvent>()
            .onEach {
                enhancementsDone++
                dollsUsedForEnhancement += it.count
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<DollDisassemblyEvent>()
            .onEach {
                disassemblesDone++
                dollsUsedForDisassembly += it.count
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<EquipDisassemblyEvent>()
            .onEach {
                equipDisassemblesDone++
                equipsUsedForDisassembly += it.count
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<RepairEvent>()
            .onEach {
                repairs += it.count
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<GameRestartEvent>()
            .onEach {
                gameRestarts++
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<CombatReportWriteEvent>()
            .onEach {
                combatReportsWritten += it.count
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<SimEnergySpentEvent>()
            .onEach {
                simEnergySpent += it.count
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
        EventBus.subscribe<CoalitionEnergySpentEvent>()
            .onEach {
                coalitionEnergySpent += it.count
                EventBus.publish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
            }.launchIn(scope)
    }

    fun reset() {
        logisticsSupportReceived = 0
        logisticsSupportSent = 0
        sortiesDone = 0
        enhancementsDone = 0
        dollsUsedForEnhancement = 0
        disassemblesDone = 0
        dollsUsedForDisassembly = 0
        equipDisassemblesDone = 0
        equipsUsedForDisassembly = 0
        repairs = 0
        gameRestarts = 0
        combatReportsWritten = 0
        simEnergySpent = 0
        coalitionEnergySpent = 0
        EventBus.tryPublish(ScriptStatsUpdateEvent(this, runner.sessionId, runner.elapsedTime))
    }

    override fun toString(): String = jacksonObjectMapper().writerWithDefaultPrettyPrinter()
        .writeValueAsString(this)
}
