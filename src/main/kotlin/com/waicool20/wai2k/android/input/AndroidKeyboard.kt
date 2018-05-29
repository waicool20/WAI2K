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

package com.waicool20.wai2k.android.input

import com.waicool20.wai2k.android.AndroidScreen
import org.sikuli.basics.Settings
import org.sikuli.script.Key
import org.sikuli.script.KeyModifier
import org.sikuli.script.Location

/**
 * Class representing a virtual keyboard for a [AndroidScreen]
 * This class is also in charge of coordinating keyboard actions between threads, unlike [AndroidRobot]
 * all functions are synchronized and thus only one thread may have access to keyboard actions.
 *
 * @property robot [AndroidRobot] used for generating actions.
 * @constructor Main constructor
 * @param robot [AndroidRobot] to use for generating actions.
 */
class AndroidKeyboard(val robot: AndroidRobot) {
    companion object {
        /**
         * Parses the given modifiers string and returns corresponding [KeyModifier].
         *
         * @param modifiers String of modifiers.
         * @return Corresponding [KeyModifier].
         */
        fun parseModifiers(modifiers: String): Int {
            var mods = 0
            modifiers.toCharArray().forEach {
                mods = mods.or(when (it) {
                    Key.C_CTRL -> KeyModifier.CTRL
                    Key.C_ALT -> KeyModifier.ALT
                    Key.C_SHIFT -> KeyModifier.SHIFT
                    Key.C_META -> KeyModifier.META
                    Key.C_ALTGR -> KeyModifier.ALTGR
                    Key.C_WIN -> KeyModifier.WIN
                    else -> 0
                })
            }
            return mods
        }
    }

    /**
     * Types text at a given location
     *
     * @param location Location to type to, can be null to just type directly into the screen.
     * @param text The text to type.
     * @param modifiers Key modifiers to press during typing.
     */
    @Synchronized
    fun type(location: Location?, text: String, modifiers: Int) = synchronized(this) {
        if (location != null) robot.screen.click(location)
        val pause = if (Settings.TypeDelay > 1) 1 else (Settings.TypeDelay * 1000).toInt()
        robot.pressModifiers(modifiers)
        text.toCharArray().map(Char::toInt).forEach {
            robot.typeKey(it)
            robot.delay(if (pause < 80) 80 else pause)
        }
        Settings.TypeDelay = 0.0
    }

    /**
     * Releases all keys
     */
    @Synchronized
    fun keyUp() = synchronized(this) { robot.keyUp() }

    /**
     * Releases a specific key.
     *
     * @param keycode The key to release.
     */
    @Synchronized
    fun keyUp(keycode: Int) = synchronized(this) { robot.keyUp(keycode) }

    /**
     * Releases the keys specified by the string.
     *
     * @param keys Keys to be released.
     */
    @Synchronized
    fun keyUp(keys: String) = synchronized(this) { robot.keyUp(keys) }

    /**
     * Presses a specific key.
     *
     * @param keycode The key to press.
     */
    @Synchronized
    fun keyDown(keycode: Int) = synchronized(this) { robot.keyDown(keycode) }

    /**
     * Presses the keys specified by the string.
     *
     * @param keys Keys to be pressed.
     */
    @Synchronized
    fun keyDown(keys: String) = synchronized(this) { robot.keyDown(keys) }

    /**
     * Executes an action atomically while keeping the synchronized lock to this object.
     * Useful if you want to do multiple actions in one go without the possibility of a thread
     * stealing ownership.
     *
     * @param action Action to execute while keeping the lock this object.
     * @return Result of [action]
     */
    @Synchronized
    inline fun <T> atomicAction(action: () -> T): T = synchronized(this) { action() }
}
