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

class LogisticsSupport private constructor(
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
                LogisticsSupport(28, of(hours = 12))
        )
    }

    class Assignments(val echelon: Echelon, val logisticSupports: List<LogisticsSupport>)
}
