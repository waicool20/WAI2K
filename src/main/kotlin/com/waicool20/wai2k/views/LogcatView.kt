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

import com.waicool20.cvauto.android.ADB
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.javafx.tooltips.fadeAfter
import com.waicool20.waicoolutils.javafx.tooltips.showAt
import com.waicool20.waicoolutils.streams.TextAreaOutputStream
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.Tooltip
import javafx.scene.input.Clipboard
import javafx.scene.layout.GridPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import tornadofx.*

class LogcatView : CoroutineScopeView() {
    override val root: GridPane by fxml("/views/console.fxml")
    private val consoleTextArea: TextArea by fxid()
    private val clearButton: Button by fxid()
    private val toTopButton: Button by fxid()
    private val toBottomButton: Button by fxid()
    private val copyButton: Button by fxid()

    private val textArea = TextAreaOutputStream(consoleTextArea)

    private var process: Process? = null

    init {
        title = "Logcat Monitor"
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
        process = ADB.execute("logcat", "Unity:V", "*:S")
        Runtime.getRuntime().addShutdownHook(Thread { process?.destroy() })
        val r =
            Regex("(\\d\\d-\\d\\d) (\\d\\d:\\d\\d:\\d\\d.\\d{3}) (\\d+) (\\d+) ([VDIWEFS]) (\\w+)\\s+: (.*)$")
        val filters = listOf(
            "UnityEngine",
            "(Filename:",
            "<Load",
            "CommonPicLoader",
            "<GetResource",
            "ResManager",
            "ResCenter",
            "System.",
            "XLua",
            "GF.Battle",
            "[ line",
            "The referenced script ",
            "Controller",
            "()",
            "Object[]",
            "Single",
            "(WWW)"
        )
        val color = Regex("<color=#\\w{6}>(.*?)</color>")
        launch(Dispatchers.IO) {
            process?.inputStream?.bufferedReader()?.forEachLine { line ->
                val match = r.matchEntire(line) ?: return@forEachLine
                var (date, time, pid, tid, level, tag, msg) = match.destructured
                if (msg.isBlank()) return@forEachLine
                if (filters.any { msg.contains(it) }) return@forEachLine
                color.matchEntire(msg)?.let { msg = it.groupValues[1] }
                launch(Dispatchers.JavaFx) {
                    textArea.writeLine("[$date $time] $msg\n")
                }
            }
        }
    }

    override fun onUndock() {
        process?.destroy()
        super.onUndock()
    }
}