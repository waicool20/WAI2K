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

package com.waicool20.wai2k.web

import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.config.Wai2kProfile.CombatReport
import com.waicool20.wai2k.config.Wai2kProfile.Logistics.ReceivalMode
import com.waicool20.wai2k.game.LogisticsSupport
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

sealed class Store {
    companion object {
        fun config(): Wai2kConfig {
            return Wai2kConfig.load()
        }

        fun profile(): Wai2kProfile {
            return Wai2kProfile.load(this.config().currentProfile)
        }

        fun profile(name: String?): Wai2kProfile {
            if (name.isNullOrEmpty()) {
                return this.profile()
            }
            return Wai2kProfile.load(name)
        }

        fun profiles(): List<String> {
            return Wai2kProfile.PROFILE_DIR.listDirectoryEntries("*.json")
                .map { it.nameWithoutExtension }
                .sorted()
        }

        val logisticsReceivalModeList = ReceivalMode.values()
            .toList()
            .sortedWith(naturalOrder())

        val assignmentList = LogisticsSupport.list.map { it.formattedString }

        val combatReportTypeList = CombatReport.Type.values().toList()
    }
}
