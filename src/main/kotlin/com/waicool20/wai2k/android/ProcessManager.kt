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

package com.waicool20.wai2k.android

import com.waicool20.cvauto.android.AndroidDevice

class ProcessManager(val device: AndroidDevice) {
    /**
     * Gets the name of the currently active activity
     */
    val currentActivity
        get() =
            device.execute("dumpsys", "window", "windows", "|", "grep", "-E", "'mCurrentFocus'")
                .inputStream.bufferedReader().readText()
                .takeLastWhile { it != ' ' }.dropLast(1)

    /**
     * Starts an app
     *
     * @param pkg Name of the package that the app belongs to
     * @param activity Name of the main activity of the app
     *
     * @return true if started successfully
     */
    fun start(pkg: String, activity: String): Boolean {
        return !device.execute("am", "start", "-n", "$pkg/$activity")
            .inputStream.bufferedReader().readText()
            .contains("Error")
    }

    /**
     * Stops an app
     *
     * @param pkg Name of the package that the app belongs to
     */
    fun kill(pkg: String) {
        device.execute("am", "force-stop", pkg)
    }
}
