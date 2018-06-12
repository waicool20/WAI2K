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

package com.waicool20.wai2k.views.tabs

import com.waicool20.wai2k.config.Configurations
import com.waicool20.wai2k.script.ScriptContext
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import tornadofx.*
import java.text.DecimalFormat
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.fixedRateTimer

class StatusTabView : View() {
    override val root: VBox by fxml("/views/tabs/status-tab.fxml")

    private val configs: Configurations by inject()
    private val scriptRunner = find<ScriptContext>().scriptRunner

    private val startTimeLabel: Label by fxid()
    private val elapsedTimeLabel: Label by fxid()

    private val logisticsSentLabel: Label by fxid()
    private val logisticsReceivedLabel: Label by fxid()

    private val prevEchelonButton: Button by fxid()
    private val currentEchelonLabel: Label by fxid()
    private val nextEchelonButton: Button by fxid()

    private val echelonLogisticsLabel: Label by fxid()
    private val echelonRepairs: Label by fxid()

    private var currentEchelon: Int = 1

    init {
        title = "Status"
        prevEchelonButton.action {
            currentEchelon = if (currentEchelon == 1) 10 else currentEchelon - 1
            updateView()
        }
        nextEchelonButton.action {
            currentEchelon = if (currentEchelon == 10) 1 else currentEchelon + 1
            updateView()
        }
    }

    override fun onDock() {
        super.onDock()
        fixedRateTimer("Status Tab View Updater", period = 1000) {
            runLater { updateView() }
        }
    }

    private fun updateView() {
        updateTimes()
        updateScriptStats()
        updateEchelonStats()
    }

    private fun updateTimes() {
        startTimeLabel.text = scriptRunner.lastStartTime?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) ?: ""
        if (scriptRunner.isRunning) elapsedTimeLabel.text = timeDelta(scriptRunner.lastStartTime)
    }

    private fun updateScriptStats() {
        // TODO Add script stats
    }

    private fun updateEchelonStats() {
        currentEchelonLabel.text = "Echelon $currentEchelon"
        scriptRunner.gameState.apply {
            val echelon = echelons[currentEchelon - 1]
            echelonLogisticsLabel.text = echelon.logisticsSupportAssignment?.let {
                val delta = timeDelta(it.eta)
                "${it.logisticSupport.chapter}-${it.logisticSupport.chapterIndex + 1} (ETA: $delta)"
            } ?: "---"
            echelonRepairs.text = echelon.members.joinToString("\n") {
                if (it.repairEta?.isAfter(ZonedDateTime.now()) == true) {
                    "${it.number}: ${timeDelta(it.repairEta)}"
                } else {
                    "${it.number}: ---"
                }
            }
        }
    }

    private fun timeDelta(time: ZonedDateTime?): String {
        val duration = time?.let { Duration.between(it, ZonedDateTime.now()).abs() }
                ?: return "00:00:00"
        return formatDuration(duration) ?: "00:00:00"
    }

    private fun formatDuration(duration: Duration?) = duration?.seconds?.let {
        String.format("%02d:%02d:%02d", it / 3600, (it % 3600) / 60, it % 60)
    }

    private fun hoursSince(time: ZonedDateTime?) =
            time?.let { Duration.between(it, ZonedDateTime.now()).seconds / 3600.0 } ?: 0.0

    private fun formatDecimal(d: Double) = DecimalFormat("0.00").format(d).replace("\uFFFD", "0.00")
}
