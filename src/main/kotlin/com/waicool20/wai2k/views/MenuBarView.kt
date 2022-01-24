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

import com.waicool20.cvauto.android.ADB
import com.waicool20.wai2k.Wai2K
import com.waicool20.wai2k.android.ProcessManager
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.game.GFL
import com.waicool20.waicoolutils.DesktopUtils
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.stage.FileChooser
import javafx.stage.StageStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tornadofx.*
import kotlin.system.exitProcess

class MenuBarView : CoroutineScopeView() {
    override val root: MenuBar by fxml("/views/menu.fxml")
    private val quitItem: MenuItem by fxid()
    private val consoleItem: MenuItem by fxid()
    private val discordItem: MenuItem by fxid()
    private val wikiItem: MenuItem by fxid()
    private val aboutItem: MenuItem by fxid()
    private val contributeItem: MenuItem by fxid()
    private val donateItem: MenuItem by fxid()
    private val toolsItem: MenuItem by fxid()
    private val openFolderItem: MenuItem by fxid()
    private val logsItem: MenuItem by fxid()
    private val homographyItem: MenuItem by fxid()
    private val logcatItem: MenuItem by fxid()
    private val restartGameItem: MenuItem by fxid()

    private val wai2KContext: Wai2KContext by inject()

    override fun onDock() {
        super.onDock()
        quitItem.setOnAction { exitProcess(0) }
        openFolderItem.setOnAction { DesktopUtils.open(Wai2K.CONFIG_DIR) }
        consoleItem.setOnAction { find<ConsoleView>().openWindow(owner = null)?.toFront() }
        aboutItem.setOnAction { find<AboutView>().openModal(stageStyle = StageStyle.UNDECORATED) }
        toolsItem.setOnAction { find<DebugView>().openWindow(owner = null)?.toFront() }
        logsItem.setOnAction { DesktopUtils.open(Wai2K.CONFIG_DIR.resolve("debug.log")) }
        discordItem.setOnAction { DesktopUtils.browse("https://discord.gg/2tt5Der") }
        contributeItem.setOnAction { DesktopUtils.browse("https://github.com/waicool20/WAI2K") }
        wikiItem.setOnAction { DesktopUtils.browse("https://github.com/waicool20/WAI2K/wiki") }
        donateItem.setOnAction { DesktopUtils.browse("https://ko-fi.com/waicool20") }
        homographyItem.setOnAction {
            val device =
                ADB.getDevices().find { it.serial == wai2KContext.wai2KConfig.lastDeviceSerial }
                    ?: return@setOnAction
            chooseFile(
                "Select base image...",
                arrayOf(FileChooser.ExtensionFilter("PNG files (*.png)", "*.png")),
                wai2KContext.wai2KConfig.assetsDirectory.toFile()
            ).firstOrNull()?.let { HomographyViewer(device, it).openWindow() }
        }
        logcatItem.setOnAction { find<LogcatView>().openWindow(owner = null)?.toFront() }
        restartGameItem.setOnAction {
            launch {
                val device =
                    ADB.getDevices().find { it.serial == wai2KContext.wai2KConfig.lastDeviceSerial }
                        ?: return@launch
                ProcessManager(device).restart(GFL.PKG_NAME)
            }
        }
    }
}
