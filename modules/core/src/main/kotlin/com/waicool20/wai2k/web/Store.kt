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

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.config.Wai2kProfile.CombatReport
import com.waicool20.wai2k.config.Wai2kProfile.CombatSimulation.Coalition.Type
import com.waicool20.wai2k.config.Wai2kProfile.CombatSimulation.Level
import com.waicool20.wai2k.config.Wai2kProfile.Logistics.ReceivalMode
import com.waicool20.wai2k.game.CombatMap
import com.waicool20.wai2k.game.LogisticsSupport
import com.waicool20.wai2k.game.TDoll
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.combat.MapRunner
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

sealed class Store {
    companion object {
        fun config(): Wai2kConfig {
            return Wai2k.config
        }

        fun profile(): Wai2kProfile {
            return Wai2kProfile.load(this.config().currentProfile)
        }

        fun profile(name: String?): Wai2kProfile {
            if (name.isNullOrEmpty()) {
                return this.profile()
            }

            config().currentProfile = name
            val profile = Wai2kProfile.load(name)
            Wai2k.scriptRunner.profile = profile

            return profile
        }

        fun profiles(): List<String> {
            val profiles = Wai2kProfile.PROFILE_DIR.listDirectoryEntries("*.json")
                .map { it.nameWithoutExtension }
                .sorted()

            if (profiles.isEmpty()) {
                return listOf("Default")
            }

            return profiles
        }

        fun maps(): Map<String, List<*>> {
            val storyMaps = MapRunner.list.keys.filterIsInstance<CombatMap.StoryMap>()
            val eventMaps = MapRunner.list.keys.filterIsInstance<CombatMap.EventMap>()
            val campaignMaps = MapRunner.list.keys.filterIsInstance<CombatMap.CampaignMap>()

            return mapOf(
                "normal" to storyMaps.filter { it.type == CombatMap.Type.NORMAL },
                "emergency" to storyMaps.filter { it.type == CombatMap.Type.EMERGENCY },
                "night" to storyMaps.filter { it.type == CombatMap.Type.NIGHT },
                "campaign" to campaignMaps,
                "event" to eventMaps,
                "logistics" to LogisticsSupport.list
            )
        }

        fun classifier(): Map<String, List<*>> {
            return mapOf(
                "logisticsReceiveModeList" to ReceivalMode.values()
                    .toList()
                    .sortedWith(naturalOrder()),
                "combatReportTypeList" to CombatReport.Type.values().toList(),
                "captureMethodList" to AndroidDevice.CaptureMethod.values().toList(),
                "compressionModeList" to AndroidDevice.CompressionMode.values().toList(),
                "dolls" to TDoll.listAll(this.config()).sortedBy { it.name },
                "combatSimData" to Level.values().toList(),
                "combatSimNeural" to listOf(Level.OFF, Level.ADVANCED),
                "combatSimCoalition" to Type.values().toList(),
                "timeStopType" to Wai2kProfile.Stop.Time.Mode.values().toList(),
            )
        }
    }
}
