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

import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.waicoolutils.DesktopUtils
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import tornadofx.*

class AboutView : View() {
    override val root: VBox by fxml("/views/about.fxml")
    private val repoLink: Hyperlink by fxid()
    private val versionText: Label by fxid()

    private val context: Wai2KContext by inject()

    init {
        title = "WAI2K - About"
    }

    override fun onDock() {
        super.onDock()
        root.setOnKeyPressed { close() }
        root.setOnMouseClicked { close() }
        versionText.text = "Commit: ${context.versionInfo.version}"
        repoLink.setOnAction {
            DesktopUtils.browse("https://github.com/waicool20/WAI2K")
        }
    }
}
