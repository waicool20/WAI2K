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

package com.waicool20.util.streams

import javafx.scene.control.TextArea
import tornadofx.*

class TextAreaOutputStream(private val console: TextArea, private val maxLines: Int = 1000) : LineBufferedOutputStream() {
    override fun writeLine(line: String) {
        runLater {
            if (line.contains("\u001b[2J\u001b[H")) {
                console.clear()
                return@runLater
            }
            if (console.text.count { it == '\n' } >= maxLines) {
                console.deleteText(0, console.text.indexOf('\n') + 1)
            }
            console.appendText(line.replace(Regex("\\u001b\\[.+?m"), ""))
        }
    }
}
