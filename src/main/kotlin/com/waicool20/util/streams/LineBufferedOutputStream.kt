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

import java.io.OutputStream

abstract class LineBufferedOutputStream : OutputStream() {
    private val buffer = StringBuffer()

    override fun write(byte: Int) {
        val char = byte.toChar()
        buffer.append(char)
        if (char == '\n') {
            flush()
        }
    }

    override fun flush() {
        writeLine(buffer.toString())
        buffer.setLength(0)
    }

    abstract fun writeLine(line: String)
}
