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

package com.waicool20.wai2k.views

import com.waicool20.wai2k.util.IOHook
import com.waicool20.waicoolutils.javafx.tooltips.fadeAfter
import com.waicool20.waicoolutils.javafx.tooltips.showAt
import com.waicool20.waicoolutils.streams.TextAreaOutputStream
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.Tooltip
import javafx.scene.input.Clipboard
import javafx.scene.layout.GridPane
import tornadofx.*

class ConsoleView : View() {
    override val root: GridPane by fxml("/views/console.fxml")
    private val consoleTextArea: TextArea by fxid()
    private val clearButton: Button by fxid()
    private val toTopButton: Button by fxid()
    private val toBottomButton: Button by fxid()
    private val copyButton: Button by fxid()


    val logs get() = consoleTextArea.text

    init {
        title = "WAI2K - Console"
        val textArea = TextAreaOutputStream(consoleTextArea)
        IOHook.hookToStdOut(textArea)
        IOHook.hookToStdErr(textArea)

        clearButton.setOnAction {
            consoleTextArea.clear()
        }
        copyButton.setOnAction {
            Clipboard.getSystemClipboard().putString(consoleTextArea.text)
            Tooltip("Copied everything!").apply {
                fadeAfter(500)
                showAt(copyButton)
            }
        }
        toTopButton.setOnAction {
            consoleTextArea.scrollTop = 0.0
        }
        toBottomButton.setOnAction {
            consoleTextArea.scrollTop = Double.MAX_VALUE
        }
    }

    override fun onDock() {
        super.onDock()
        setWindowMinSize(1000, 400)
        modalStage?.apply {
            x = primaryStage.x + primaryStage.width + 10
            y = primaryStage.y
            height = primaryStage.height
            width = 1000.0
        }
    }
}
