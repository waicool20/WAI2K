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

package com.waicool20.wai2k.views.tabs.preferences

import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.util.YuuBot
import com.waicool20.waicoolutils.DesktopUtils
import com.waicool20.waicoolutils.javafx.listen
import com.waicool20.waicoolutils.javafx.listenDebounced
import javafx.scene.control.CheckBox
import javafx.scene.control.Hyperlink
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import tornadofx.*


class YuuBotView : View() {
    override val root: VBox by fxml("/views/tabs/preferences/yuubot.fxml")
    private val apiKeyTextField: TextField by fxid()
    private val setupHyperlink: Hyperlink by fxid()
    private val onRestartCheckBox: CheckBox by fxid()
    private val onStopConditionCheckBox: CheckBox by fxid()

    private val context: Wai2KContext by inject()

    override fun onDock() {
        super.onDock()
        setupHyperlink.action {
            DesktopUtils.browse("https://github.com/waicool20/WAI2K/wiki/Discord-Integration")
        }
        apiKeyTextField.textProperty().apply {
            listen { apiKeyTextField.style = "-fx-border-color: yellow; -fx-border-width: 2px" }
            listenDebounced(1000, "ApiKeyTextFieldProperty") { newVal ->
                testApiKey(newVal)
            }
        }

        apiKeyTextField.text = context.wai2KConfig.apiKey

        with(context.wai2KConfig.notificationsConfig) {
            onRestartCheckBox.bind(onRestartProperty)
            onStopConditionCheckBox.bind(onStopConditionProperty)
        }
    }

    fun testApiKey(apiKey: String) {
        apiKeyTextField.style = "-fx-border-color: yellow; -fx-border-width: 2px"
        YuuBot.testApiKey(apiKey) { status ->
            context.wai2KConfig.apiKey = when (status) {
                YuuBot.ApiKeyStatus.VALID -> {
                    apiKeyTextField.style = "-fx-border-color: lightgreen; -fx-border-width: 2px"
                    apiKey
                }
                YuuBot.ApiKeyStatus.INVALID -> {
                    apiKeyTextField.style = "-fx-border-color: red; -fx-border-width: 2px"
                    ""
                }
                YuuBot.ApiKeyStatus.UNKNOWN -> apiKey
            }
        }
    }
}