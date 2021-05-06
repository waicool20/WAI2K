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

import com.sun.javafx.css.StyleManager
import javafx.application.Application
import tornadofx.*
import kotlin.system.exitProcess

class Wai2KWorkspace : Workspace() {

    companion object {
        fun setDarkMode(en: Boolean) {
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA)
            val sm = StyleManager.getInstance()
            if (en) {
                sm.addUserAgentStylesheet("/css/dark.css")
            } else {
                sm.removeUserAgentStylesheet("/css/dark.css")
            }
        }
    }

    init {
        add(MenuBarView::class)
        setWindowMinSize(560, 700)
        setWindowMaxSize(560, 700)
    }

    override fun onDock() {
        super.onDock()
        dock<MainView>()
    }

    override fun onUndock() {
        super.onUndock()
        exitProcess(0)
    }
}
