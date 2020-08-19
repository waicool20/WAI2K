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

package com.waicool20.wai2k.views.tabs.profile.stop

import com.waicool20.wai2k.views.tabs.profile.AbstractProfileView
import com.waicool20.waicoolutils.DurationUtils
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.javafx.setAllowedCharacters
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import kotlin.math.floor
import kotlin.math.roundToLong

class TimeStopElapsedTimeView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/profile/stop/time-stop-elapsedtime.fxml")
    private val daysField: TextField by fxid()
    private val hoursField: TextField by fxid()
    private val minutesField: TextField by fxid()
    private val secondsField: TextField by fxid()

    override fun setValues() {
        val chars = "1234567890"
        daysField.setAllowedCharacters(chars)
        hoursField.setAllowedCharacters(chars)
        minutesField.setAllowedCharacters(chars)
        secondsField.setAllowedCharacters(chars)
    }

    override fun createBindings() {
        var secondsLeft = context.currentProfile.stop.time.elapsedTime.seconds
        daysField.text = floor(secondsLeft / 86400.0).roundToLong().toString()
        secondsLeft %= 86400
        hoursField.text = floor(secondsLeft / 3600.0).roundToLong().toString()
        secondsLeft %= 3600
        minutesField.text = floor(secondsLeft / 60.0).roundToLong().toString()
        secondsLeft %= 60
        secondsField.text = secondsLeft.toString()

        daysField.textProperty().addListener("DaysFieldListener") { _ -> updateElapsedTime() }
        hoursField.textProperty().addListener("HoursFieldListener") { _ -> updateElapsedTime() }
        minutesField.textProperty().addListener("MinutesFieldListener") { _ -> updateElapsedTime() }
        secondsField.textProperty().addListener("SecondsFieldListener") { _ -> updateElapsedTime() }
    }

    private fun updateElapsedTime() {
        context.currentProfile.stop.time.elapsedTime = DurationUtils.of(
            days = daysField.text.toLongOrNull() ?: 0,
            hours = hoursField.text.toLongOrNull() ?: 0,
            minutes = minutesField.text.toLongOrNull() ?: 0,
            seconds = secondsField.text.toLongOrNull() ?: 0
        )
    }
}