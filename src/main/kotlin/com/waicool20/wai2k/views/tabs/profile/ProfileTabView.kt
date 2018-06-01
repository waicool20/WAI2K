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

import com.waicool20.util.javafx.addListener
import com.waicool20.wai2k.config.Configurations
import javafx.geometry.Pos
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import org.controlsfx.control.MasterDetailPane
import tornadofx.*

class ProfileTabView : View() {
    override val root: HBox by fxml("/views/tabs/profile/profile-tab.fxml")
    private val profileTreeView: TreeView<String> by fxid()
    private val profilePane: MasterDetailPane by fxid()

    private val configs: Configurations by inject()

    init {
        title = "Profile"
        initializeTree()
        profilePane.dividerPosition = 1.0
    }

    fun initializeTree() {
        profileTreeView.root = TreeItem<String>().apply {
            valueProperty().bind(configs.currentProfile.nameProperty)
            isExpanded = true
        }
        ProfileViewMappings.list.forEach { node ->
            node.isExpanded = true
            if (node.parent == null) {
                profileTreeView.root.children.add(node)
            } else {
                ProfileViewMappings.list.find { it.view == node.parent }?.children?.add(node)
            }
        }
        profileTreeView.focusModel.focusedItemProperty().addListener("ProfileFocused") { newVal ->
            profilePane.apply {
                if (newVal is ProfileViewMappings.ViewNode) {
                    val pos = profilePane.dividerPosition
                    masterNode = find(newVal.view).root
                    dividerPosition = pos
                    isAnimated = true
                    isShowDetailNode = true
                } else {
                    masterNode = hbox(alignment = Pos.CENTER) {
                        label("Choose something to configure on the right!")
                    }
                    isAnimated = false
                    isShowDetailNode = false
                }
            }
        }
    }
}
