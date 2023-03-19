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

package com.waicool20.wai2k.util

import com.waicool20.waicoolutils.streams.TeeOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream


object IOHook {
    private class OutputStreamGroup : OutputStream() {
        private val streams = mutableListOf<OutputStream>()
        override fun write(b: Int) {
            streams.forEach {
                try {
                    it.write(b)
                } catch (e: IOException) {
                    streams.remove(it)
                }
            }
        }

        fun add(os: OutputStream) {
            streams.add(os)
        }
    }

    private val outGroup = OutputStreamGroup()
    private val errGroup = OutputStreamGroup()

    init {
        System.setOut(PrintStream(TeeOutputStream(System.out, outGroup)))
        System.setErr(PrintStream(TeeOutputStream(System.err, errGroup)))
    }

    fun hookToStdOut(os: OutputStream) {
        outGroup.add(os)
    }

    fun hookToStdErr(os: OutputStream) {
        errGroup.add(os)
    }
}
