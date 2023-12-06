/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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

package com.waicool20.wai2k.android

import com.waicool20.cvauto.android.AndroidDevice
import java.util.concurrent.TimeUnit

class ProcessManager(val device: AndroidDevice) {
    /**
     * Gets the name of the currently active activity
     */
    val currentActivity
        get(): String {
            val regex = Regex("ActivityRecord\\{.+ .+ (.*) .+}")
            val text = device.executeShell(
                "dumpsys", "activity", "activities",
                "|", "grep", "-E", "'ResumedActivity'"
            ).inputStream.bufferedReader().readText()
            return regex.find(text)?.groupValues?.get(1) ?: ""
        }

    /**
     * Starts an app
     *
     * @param pkg Name of the package that the app belongs to
     *
     * @return true if started successfully
     */
    fun start(pkg: String): Boolean {
        val monkey = device.execute("monkey", "-p", pkg, "1", "&&", "echo", "ok")
        if (monkey.inputStream.bufferedReader().readText().contains("ok")) return true
        val regex = Regex(".+ ($pkg/.*?) .* .*")
        val activityName = device.execute("pm dump $pkg | grep -A1 'action.MAIN'")
            .inputStream.bufferedReader().readLines()
            .firstNotNullOfOrNull { regex.matchEntire(it) }
            ?.groupValues?.get(1) ?: return false
        return device.execute("am", "start", "-n", activityName).inputStream.bufferedReader()
            .readText().contains("Starting")
    }

    /**
     * Stops an app
     *
     * @param pkg Name of the package that the app belongs to
     */
    fun kill(pkg: String) {
        device.execute("am", "force-stop", pkg)
    }

    /**
     * Restart an app
     *
     * @param pkg Name of the package that the app belongs to
     * @param delay Delay millis after killing app to start
     */
    fun restart(pkg: String, delay: Long = 5000) {
        kill(pkg)
        TimeUnit.MILLISECONDS.sleep(delay)
        start(pkg)
    }
}
