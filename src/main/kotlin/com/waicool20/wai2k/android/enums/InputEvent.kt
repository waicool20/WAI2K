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

/**
 * Represents a linux kernel event, see
 * [Linux Input Events](https://github.com/torvalds/linux/blob/master/include/uapi/linux/input-event-codes.h)
 * for more information.
 *
 * @param code Code value of this event
 */
enum class InputEvent(val code: Long) {
    /* Synchronization Events */
    SYN_REPORT(0),
    SYN_CONFIG(1),
    SYN_MT_REPORT(2),
    SYN_DROPPED(3),
    SYN_MAX(0xf),
    SYN_CNT(SYN_MAX.code + 1),

    /* Touch Events */
    ABS_MT_SLOT(0x2f),
    ABS_MT_TOUCH_MAJOR(0x30),
    ABS_MT_TOUCH_MINOR(0x31),
    ABS_MT_WIDTH_MAJOR(0x32),
    ABS_MT_WIDTH_MINOR(0x33),
    ABS_MT_ORIENTATION(0x34),
    ABS_MT_POSITION_X(0x35),
    ABS_MT_POSITION_Y(0x36),
    ABS_MT_TOOL_TYPE(0x37),
    ABS_MT_BLOB_ID(0x38),
    ABS_MT_TRACKING_ID(0x39),
    ABS_MT_PRESSURE(0x3a),
    ABS_MT_DISTANCE(0x3b),
    ABS_MT_TOOL_X(0x3c),
    ABS_MT_TOOL_Y(0x3d);

    companion object {
        fun findByCode(code: Long) = InputEvent.values().find { it.code == code }
    }
}
