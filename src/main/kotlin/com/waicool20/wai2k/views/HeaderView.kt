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

import com.waicool20.wai2k.Wai2K
import com.waicool20.wai2k.config.Wai2KContext
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptContext
import com.waicool20.waicoolutils.javafx.AlertFactory
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.javafx.tooltips.TooltipSide
import com.waicool20.waicoolutils.javafx.tooltips.fadeAfter
import com.waicool20.waicoolutils.javafx.tooltips.showAt
import com.waicool20.waicoolutils.logging.loggerFor
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.SplitMenuButton
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import java.nio.file.Files
import kotlin.streams.toList

class HeaderView : CoroutineScopeView() {
    override val root: HBox by fxml("/views/header.fxml")

    private val PLAY_GLYPH = Glyph("FontAwesome", FontAwesome.Glyph.PLAY)
    private val PAUSE_GLYPH = Glyph("FontAwesome", FontAwesome.Glyph.PAUSE)
    private val logger = loggerFor<HeaderView>()

    private val profileComboBox: ComboBox<String> by fxid()
    private val startPauseButton: SplitMenuButton by fxid()
    private val stopButton: Button by fxid()

    private val wai2KContext: Wai2KContext by inject()
    private val scriptRunner by lazy {
        find<ScriptContext>().scriptRunner
    }

    val buttons: HBox by fxid()

    override fun onDock() {
        super.onDock()
        profileComboBox.setOnShowing { updateProfileItems() }
        profileComboBox.setOnAction { selectProfile() }
        startPauseButton.setOnAction { onStartPause() }
        stopButton.setOnAction { scriptRunner.stop() }
        createBindings()
    }

    override fun onSave() {
        super.onSave()
        wai2KContext.currentProfile.apply {
            save()
            AlertFactory.info(content = "Profile $name was saved!").showAndWait()
        }
    }

    override fun onDelete() {
        super.onDelete()
        wai2KContext.currentProfile.apply {
            val toDelete = name
            confirm(
                header = "Delete profile [$toDelete]?",
                title = "Wai2K - Profile Deletion Confirmation"
            ) {
                delete()
                profileComboBox.value = ""
                AlertFactory.info(content = "Profile $toDelete was deleted").showAndWait()
            }
        }
    }

    override fun onRefresh() {
        super.onRefresh()
        launch(Dispatchers.IO) {
            val profile = Wai2KProfile.load(wai2KContext.currentProfile.path)
            withContext(Dispatchers.JavaFx) { setNewProfile(profile) }
        }
    }

    private fun createBindings() {
        profileComboBox.bind(wai2KContext.currentProfile.nameProperty)
        wai2KContext.currentProfileProperty.addListener("HeaderViewProfile") { newVal ->
            createBindings()
            launch {
                Tooltip("Profile ${newVal.name} has been loaded!").apply {
                    fadeAfter(700)
                    showAt(profileComboBox, TooltipSide.TOP_LEFT)
                }
            }
        }
        startPauseButton.textProperty().addListener("StartStopButtonListener") { newVal ->
            startPauseButton.apply {
                val color = if (newVal == "Pause") "yellow" else "green"
                styleClass.removeAll { it.endsWith("-split-menu") }
                styleClass.add("$color-split-menu")
            }
            startPauseButton.graphic = if (newVal == "Pause") PAUSE_GLYPH else PLAY_GLYPH
        }
    }

    private fun selectProfile() {
        val newProfile = profileComboBox.value
        if (Wai2KProfile.profileExists(newProfile)) {
            launch(Dispatchers.IO) {
                val profile = Wai2KProfile.load(newProfile)
                withContext(Dispatchers.JavaFx) { setNewProfile(profile) }
            }
        }
    }

    private fun setNewProfile(profile: Wai2KProfile) {
        wai2KContext.apply {
            wai2KConfig.currentProfile = profile.name
            wai2KConfig.save()
            currentProfileProperty.set(profile)
        }
    }

    private fun updateProfileItems() {
        val currentProfile = profileComboBox.value
        val profiles = Files.walk(Wai2KProfile.PROFILE_DIR).toList()
            .filter { Files.isRegularFile(it) }
            .map { "${it.fileName}".removeSuffix(Wai2K.CONFIG_SUFFIX) }
            .filter { it != currentProfile }
            .sorted()
        if (profiles.isNotEmpty()) {
            profileComboBox.items.setAll(profiles)
        }
    }

    private fun onStartPause() = launch {
        if (scriptRunner.isRunning) {
            if (scriptRunner.isPaused) {
                scriptRunner.isPaused = false
                startPauseButton.text = "Pause"
            } else {
                scriptRunner.isPaused = true
                startPauseButton.text = "Cont."
                logger.info("Script will pause when the current cycle ends")
            }
        } else {
            scriptRunner.apply {
                config = wai2KContext.wai2KConfig
                profile = wai2KContext.currentProfile
            }.run()
            startPauseButton.text = "Pause"
            stopButton.show()
            startScriptMonitor()
        }
    }


    private fun onStop() = launch {
        startPauseButton.text = "Start"
        stopButton.hide()
    }

    private fun startScriptMonitor() = launch(Dispatchers.IO) {
        scriptRunner.join()
        onStop()
    }
}
