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

package com.waicool20.wai2k.views

import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.events.StartupCompleteEvent
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.logging.LoggingEventBus
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class LoaderView : CoroutineScopeView(), LoggingEventBus.Listener {
    override val root: AnchorPane by fxml("/views/loader.fxml")
    private val statusLabel: Label by fxid()

    init {
        title = "WAI2K - Startup"
        statusLabel.text = ""
    }

    override fun onDock() {
        super.onDock()
        LoggingEventBus.subscribe(Regex("(.* - (.*)|.*)"), this)
        find<ConsoleView>()
        EventBus.subscribe<StartupCompleteEvent>()
            .take(1).onCompletion { closeAndShowMainApp() }
            .launchIn(this)
    }

    override fun onLogEvent(match: MatchResult) {
        val text = match.groupValues[2].takeIf { it.isNotBlank() }
            ?: match.groupValues[1].takeIf { it.isNotBlank() } ?: return
        launch { statusLabel.text = text }
    }

    override fun onUndock() {
        LoggingEventBus.unsubscribe(this)
        super.onUndock()
    }

    private fun closeAndShowMainApp() {
        launch {
            delay(500)
            close()
            primaryStage.show()
            if (Wai2k.config.showConsoleOnStart) {
                find<ConsoleView>().openWindow(owner = null)
            }
            Wai2kWorkspace.setDarkMode(Wai2k.config.appearanceConfig.darkMode)
        }
    }
}
