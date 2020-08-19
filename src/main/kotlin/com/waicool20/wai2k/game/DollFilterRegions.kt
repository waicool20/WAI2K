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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.wai2k.game.TDoll.Type

class DollFilterRegions(region: AndroidRegion) {
    /**
     * 'Filter By' button
     */
    val filter = region.subRegion(1797, 368, 193, 121)

    /**
     * Map of 1-5 star rating and their regions
     */
    val starRegions = mapOf(
        6 to region.subRegion(917, 215, 256, 117),
        5 to region.subRegion(1188, 215, 256, 117),
        4 to region.subRegion(1459, 215, 256, 117),
        3 to region.subRegion(917, 349, 256, 117),
        2 to region.subRegion(1188, 349, 256, 117),
        1 to region.subRegion(1459, 349, 256, 117)
    )

    /**
     * Map of individual gun types and their regions
     */
    val typeRegions = mapOf(
        Type.HG to region.subRegion(917, 537, 256, 117),
        Type.SMG to region.subRegion(1188, 537, 256, 117),
        Type.RF to region.subRegion(1459, 537, 256, 117),
        Type.AR to region.subRegion(917, 672, 256, 117),
        Type.MG to region.subRegion(1188, 672, 256, 117),
        Type.SG to region.subRegion(1459, 672, 256, 117)
    )

    /**
     * Reset button
     */
    val reset = region.subRegion(902, 980, 413, 83)

    /**
     * Confirm button
     */
    val confirm = region.subRegion(1318, 980, 413, 83)
}