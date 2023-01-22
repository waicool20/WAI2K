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

package com.waicool20.wai2k.game.location

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.waicool20.wai2k.script.Asset

/**
 * Represents a link to another game location
 *
 * @param dest Destination of this link
 * @param asset The corresponding asset that should be clicked to get to [dest]
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Link(val dest: LocationId, val asset: Asset = EMPTY_ASSET) {
    companion object {
        private val EMPTY_ASSET = Asset()
    }

    val skippable = asset == EMPTY_ASSET
    override fun toString() = "Link(dest=$dest)"
}
