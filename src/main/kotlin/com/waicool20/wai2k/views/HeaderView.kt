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

import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.config.Wai2kProfile
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.events.ScriptStopEvent
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.waicoolutils.javafx.AlertFactory
import com.waicool20.waicoolutils.javafx.CoroutineScopeView
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.javafx.tooltips.TooltipSide
import com.waicool20.waicoolutils.javafx.tooltips.fadeAfter
import com.waicool20.waicoolutils.javafx.tooltips.showAt
import com.waicool20.wai2k.util.loggerFor
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.SplitMenuButton
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

class HeaderView : CoroutineScopeView() {
    override val root: HBox by fxml("/views/header.fxml")

    private val PLAY_GLYPH = Glyph("FontAwesome", FontAwesome.Glyph.PLAY)
    private val PAUSE_GLYPH = Glyph("FontAwesome", FontAwesome.Glyph.PAUSE)
    private val logger = loggerFor<HeaderView>()

    private val profileComboBox: ComboBox<String> by fxid()
    private val startPauseButton: SplitMenuButton by fxid()
    private val stopButton: Button by fxid()

    private val scriptRunner get() = Wai2k.scriptRunner

    val buttons: HBox by fxid()

    override fun onDock() {
        super.onDock()
        profileComboBox.setOnShowing { updateProfileItems() }
        profileComboBox.setOnAction { selectProfile() }
        startPauseButton.setOnAction { onStartPause() }
        stopButton.setOnAction { scriptRunner.stop() }
        createBindings()
        EventBus.subscribe<ScriptStopEvent>().onEach {
            startPauseButton.text = "Start"
            stopButton.hide()
        }.launchIn(this)
    }

    override fun onSave() {
        super.onSave()
        Wai2k.profile.apply {
            save()
            AlertFactory.info(content = "Profile $name was saved!").showAndWait()
        }
    }

    override fun onDelete() {
        super.onDelete()
        Wai2k.profile.apply {
            val toDelete = name
            confirm(
                header = "Delete profile [$toDelete]?",
                title = "WAI2K - Profile Deletion Confirmation"
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
            val profile = Wai2kProfile.load(Wai2k.profile.path)
            withContext(Dispatchers.JavaFx) { setNewProfile(profile) }
        }
    }

    private fun createBindings() {
        profileComboBox.bind(Wai2k.profile.nameProperty)
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
        if (Wai2kProfile.profileExists(newProfile)) {
            launch(Dispatchers.IO) {
                val profile = Wai2kProfile.load(newProfile)
                setNewProfile(profile)
            }
        }
    }

    private fun setNewProfile(profile: Wai2kProfile) {
        Wai2k.config.currentProfile = profile.name
        Wai2k.config.save()
        Wai2k.profile = profile
        launch(Dispatchers.JavaFx) {
            createBindings()
            Tooltip("Profile ${profile.name} has been loaded!").apply {
                fadeAfter(700)
                showAt(profileComboBox, TooltipSide.TOP_LEFT)
            }
        }
    }

    private fun updateProfileItems() {
        val currentProfile = profileComboBox.value
        val profiles = Wai2kProfile.PROFILE_DIR.listDirectoryEntries("*.json")
            .map { it.nameWithoutExtension }
            .filter { it != currentProfile }
            .sorted()
        if (profiles.isNotEmpty()) {
            profileComboBox.items.setAll(profiles)
        }
    }

    private fun onStartPause() = launch {
        when (scriptRunner.state) {
            ScriptRunner.State.RUNNING -> {
                scriptRunner.pause()
                startPauseButton.text = "Cont."
                logger.info("Script will pause when the current cycle ends")
            }
            ScriptRunner.State.PAUSING, ScriptRunner.State.PAUSED -> {
                scriptRunner.unpause()
                startPauseButton.text = "Pause"
            }
            ScriptRunner.State.STOPPED -> {
                scriptRunner.apply {
                    config = Wai2k.config
                    profile = Wai2k.profile
                }.run()
                startPauseButton.text = "Pause"
                stopButton.show()
            }
        }
    }
}
