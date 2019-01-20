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
import com.waicool20.waicoolutils.javafx.addListener
import com.waicool20.waicoolutils.javafx.asTimeSpinner
import javafx.scene.control.Spinner
import javafx.scene.layout.VBox
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class TimeStopSpecificTimeView : AbstractProfileView() {
    override val root: VBox by fxml("/views/tabs/profile/stop/time-stop-specifictime.fxml")
    private val hourSpinner: Spinner<Int> by fxid()
    private val minuteSpinner: Spinner<Int> by fxid()

    override fun setValues() {
        hourSpinner.asTimeSpinner(TimeUnit.HOURS)
        minuteSpinner.asTimeSpinner(TimeUnit.MINUTES)
    }

    override fun createBindings() {
        with(context.currentProfile.stop.time.specificTime) {
            hourSpinner.valueFactory.value = hour
            minuteSpinner.valueFactory.value = minute
        }

        hourSpinner.valueProperty().addListener("$javaClass HourSpinner") { _ -> updateSpecificTime() }
        minuteSpinner.valueProperty().addListener("$javaClass MinuteSpinner") { _ -> updateSpecificTime() }
    }

    private fun updateSpecificTime() {
        context.currentProfile.stop.time.specificTime = LocalTime.of(hourSpinner.value, minuteSpinner.value)
    }
}