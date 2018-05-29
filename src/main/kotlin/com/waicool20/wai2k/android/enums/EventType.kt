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

package com.waicool20.wai2k.android.enums

enum class EventType(val code: Long) {
    EV_SYN(0x00),
    EV_KEY(0x01),
    EV_REL(0x02),
    EV_ABS(0x03),
    EV_MSC(0x04),
    EV_SW(0x05),
    EV_LED(0x11),
    EV_SND(0x12),
    EV_REP(0x14),
    EV_FF(0x15),
    EV_PWR(0x16),
    EV_FF_STATUS(0x17),
    EV_MAX(0x1f),
    EV_CNT(EV_MAX.code + 1)
}
