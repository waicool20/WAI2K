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

package com.waicool20.wai2k.game

import com.fasterxml.jackson.annotation.JsonIgnore
import com.waicool20.waicoolutils.DurationUtils.of
import java.time.Duration
import java.time.Instant

data class LogisticsSupport private constructor(
    val number: Int,
    @JsonIgnore val duration: Duration
) {
    companion object {
        val list = listOf(
            // Chapter 0
            LogisticsSupport(1, of(minutes = 50)),
            LogisticsSupport(2, of(hours = 3)),
            LogisticsSupport(3, of(hours = 12)),
            LogisticsSupport(4, of(hours = 24)),
            // Chapter 1
            LogisticsSupport(5, of(minutes = 15)),
            LogisticsSupport(6, of(minutes = 30)),
            LogisticsSupport(7, of(hours = 1)),
            LogisticsSupport(8, of(hours = 2)),
            // Chapter 2
            LogisticsSupport(9, of(minutes = 40)),
            LogisticsSupport(10, of(hours = 1, minutes = 30)),
            LogisticsSupport(11, of(hours = 4)),
            LogisticsSupport(12, of(hours = 6)),
            // Chapter 3
            LogisticsSupport(13, of(minutes = 20)),
            LogisticsSupport(14, of(minutes = 45)),
            LogisticsSupport(15, of(hours = 1, minutes = 30)),
            LogisticsSupport(16, of(hours = 5)),
            // Chapter 4
            LogisticsSupport(17, of(hours = 1)),
            LogisticsSupport(18, of(hours = 2)),
            LogisticsSupport(19, of(hours = 6)),
            LogisticsSupport(20, of(hours = 8)),
            // Chapter 5
            LogisticsSupport(21, of(minutes = 30)),
            LogisticsSupport(22, of(hours = 2, minutes = 30)),
            LogisticsSupport(23, of(hours = 4)),
            LogisticsSupport(24, of(hours = 7)),
            // Chapter 6
            LogisticsSupport(25, of(hours = 2)),
            LogisticsSupport(26, of(hours = 3)),
            LogisticsSupport(27, of(hours = 5)),
            LogisticsSupport(28, of(hours = 12)),
            // Chapter 7
            LogisticsSupport(29, of(hours = 2, minutes = 30)),
            LogisticsSupport(30, of(hours = 4)),
            LogisticsSupport(31, of(hours = 5, minutes = 30)),
            LogisticsSupport(32, of(hours = 8)),
            // Chapter 8
            LogisticsSupport(33, of(hours = 1)),
            LogisticsSupport(34, of(hours = 3)),
            LogisticsSupport(35, of(hours = 6)),
            LogisticsSupport(36, of(hours = 9)),
            // Chapter 9
            LogisticsSupport(37, of(minutes = 30)),
            LogisticsSupport(38, of(hours = 1, minutes = 30)),
            LogisticsSupport(39, of(hours = 4, minutes = 30)),
            LogisticsSupport(40, of(hours = 7)),
            // Chapter 10
            LogisticsSupport(41, of(minutes = 40)),
            LogisticsSupport(42, of(hours = 1, minutes = 40)),
            LogisticsSupport(43, of(hours = 5, minutes = 20)),
            LogisticsSupport(44, of(hours = 10)),
            // Chapter 11
            LogisticsSupport(45, of(hours = 4)),
            LogisticsSupport(46, of(hours = 4)),
            LogisticsSupport(47, of(hours = 8)),
            LogisticsSupport(48, of(hours = 10))
        )
    }

    val chapter = (number - 1) / 4
    val chapterIndex = (number - 1) % 4
    val formattedString = String.format("No. %02d:  (%d - %d)", number, chapter, chapterIndex + 1)

    class Assignment(val logisticSupport: LogisticsSupport, val eta: Instant)
}
