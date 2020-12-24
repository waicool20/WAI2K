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

package com.waicool20.wai2k.views

import com.waicool20.wai2k.views.tabs.DeviceTabView
import com.waicool20.wai2k.views.tabs.StatusTabView
import com.waicool20.wai2k.views.tabs.preferences.PreferencesTabView
import com.waicool20.wai2k.views.tabs.profile.ProfileTabView
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import tornadofx.*

class MainView : View() {
    override val root: TabPane by fxml("/views/main.fxml")

    init {
        title = "WAI2K - Girls Frontline Automation Tool"
        addStageIcon(Image("/images/wai2k-icon.png"))
        root.apply {
            tab(StatusTabView::class)
            tab(DeviceTabView::class)
            tab(ProfileTabView::class)
            tab(PreferencesTabView::class)
        }
        setupHeader()
    }

    private fun setupHeader() {
        workspace.apply {
            header.items.removeAll(backButton, forwardButton, createButton)
            val buttons = header.items.toList()
            val headerView = find<HeaderView>()
            header.items.setAll(headerView.root)
            headerView.buttons.children.addAll(buttons)
            showHeadingLabel = false
        }
        forwardWorkspaceActions(find<HeaderView>())
    }
}

