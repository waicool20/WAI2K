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

package com.waicool20.wai2k.views.tabs.profile

import com.waicool20.wai2k.views.ViewNode
import com.waicool20.wai2k.views.tabs.profile.combat.CombatView
import com.waicool20.wai2k.views.tabs.profile.combat.DraggersView
import com.waicool20.wai2k.views.tabs.profile.logistics.AssignmentsView
import com.waicool20.wai2k.views.tabs.profile.logistics.LogisticsView
import com.waicool20.wai2k.views.tabs.profile.stop.CountStopView
import com.waicool20.wai2k.views.tabs.profile.stop.StopView
import com.waicool20.wai2k.views.tabs.profile.stop.TimeStopView

object ProfileViewMappings {
    val list = listOf(
        ViewNode("Logistics", LogisticsView::class),
        ViewNode("Assignments", AssignmentsView::class, parent = LogisticsView::class),
        ViewNode("Combat", CombatView::class),
        ViewNode("Draggers", DraggersView::class, parent = CombatView::class),
        ViewNode("Combat Report", CombatReportView::class),
        ViewNode("Combat Simulation", CombatSimulationView::class),
        ViewNode("Factory", FactoryView::class),
        ViewNode("Stop", StopView::class),
        ViewNode("Time", TimeStopView::class, parent = StopView::class),
        ViewNode("Count", CountStopView::class, parent = StopView::class)
    )
}
