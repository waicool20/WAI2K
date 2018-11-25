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
import org.sikuli.script.Location
import kotlin.math.*

/**
 * Class representing a virtual touch interface for a [AndroidScreen]
 * This class is also in charge of coordinating touch actions between threads, unlike [AndroidRobot]
 * all functions are synchronized and thus only one thread may have access to keyboard actions.
 *
 * @property robot [AndroidRobot] used for generating actions.
 * @constructor Main constructor
 * @param robot [AndroidRobot] to use for generating actions.
 */
class AndroidTouchInterface(val robot: AndroidRobot) {
    /**
     * Sends a tap at a given location
     *
     * @param location Location to tap.
     * @param modifiers Key modifiers to press during clicking.
     */
    @Synchronized
    fun tap(slot: Int, location: Location, modifiers: Int) = synchronized(this) {
        moveTo(slot, location)
        val pause = min(1.0, Settings.ClickDelay) * 1000
        robot.pressModifiers(modifiers)
        touchDown(slot)
        robot.delay(pause.toInt())
        touchUp(slot)
        robot.releaseModifiers(modifiers)
        Settings.ClickDelay = 0.0
    }

    /**
     * Sends a double tap at a given location
     *
     * @param location Location to tap.
     * @param modifiers Key modifiers to press during tapping.
     */
    @Synchronized
    fun doubleTap(slot: Int, location: Location, modifiers: Int) = synchronized(this) {
        repeat(2) { tap(slot, location, modifiers) }
    }

    /**
     * Moves the cursor to a given location.
     *
     * @param location The location to move the cursor to.
     */
    @Synchronized
    fun moveTo(slot: Int, location: Location) =
            synchronized(this) { robot.smoothTouchMove(listOf(AndroidRobot.Swipe(slot, dest = location))) }

    /* Low level actions */

    /**
     * Touches the screen.
     *
     */
    @Synchronized
    fun touchDown(slot: Int) =
            synchronized(this) { robot.lowLevelTouchActions { touchDown(slot) } }

    /**
     * Releases the touch on the screen.
     *
     */
    @Synchronized
    fun touchUp(slot: Int) = synchronized(this) { robot.lowLevelTouchActions { touchUp(slot) } }

    /**
     * Initiates a single touch swipe action. (Moves cursor to a location and presses without releasing)
     *
     * @param location Location to start the swipe action
     * @param resetDelays Whether or not to reset delays after pressing.
     */
    @Synchronized
    fun startSwipe(slot: Int, location: Location, resetDelays: Boolean = true) = synchronized(this) {
        moveTo(slot, location)
        robot.delay((Settings.DelayBeforeMouseDown * 1000).toInt())
        touchDown(slot)
        robot.delay(max(Settings.DelayBeforeDrag, 0.0).toInt() * 1000)
        if (resetDelays) resetSwipeDelays()
    }

    /**
     * Ends a single touch swipe action. (Moves mouse to a location and releases the button)
     *
     * @param location Location to release the swipe.
     * @param resetDelays Whether or not to reset delays after releasing.
     */
    @Synchronized
    fun endSwipe(slot: Int, location: Location, resetDelays: Boolean = true) = synchronized(this) {
        moveTo(slot, location)
        robot.delay((Settings.DelayBeforeDrop * 1000).toInt())
        touchUp(slot)
        if (resetDelays) resetSwipeDelays()
    }

    /**
     * Same as calling [startSwipe] with [loc1] then [endSwipe] with [loc2] but does this all at once while
     * keeping the lock to this object. Single touch only
     *
     * @param loc1 Location to begin startSwipe.
     * @param loc2 Location to drop at.
     */
    @Synchronized
    fun swipe(slot: Int, loc1: Location, loc2: Location) = synchronized(this) {
        startSwipe(slot, loc1, false)
        endSwipe(slot, loc2)
    }

    /**
     * Starts a pinch gesture
     *
     * @param fromRadius Radius to start the pinch gesture at
     * @param toRadius Radius to stop the pinch gesture at
     * @param angle Angle of pinch gesture
     * @param ms Length of time to complete this action within in milliseconds.
     */
    @Synchronized
    fun pinch(centerPoint: Location, fromRadius: Int, toRadius: Int, angle: Double = 0.0, ms: Long = (Settings.MoveMouseDelay * 1000).toLong()) {
        val rad = (angle * PI) / 180
        val src0 = centerPoint.offset((fromRadius * cos(rad)).roundToInt(), (fromRadius * sin(rad)).roundToInt())
        val src1 = centerPoint.offset((-fromRadius * cos(rad)).roundToInt(), (-fromRadius * sin(rad)).roundToInt())
        robot.smoothTouchMove(listOf(
                AndroidRobot.Swipe(0, dest = src0),
                AndroidRobot.Swipe(1, dest = src1)
        ))
        robot.lowLevelTouchActions {
            touchDown(0)
            touchDown(1)
        }
        robot.smoothTouchMove(listOf(
                AndroidRobot.Swipe(0, src = src0, dest = centerPoint.offset((toRadius * cos(rad)).roundToInt(), (toRadius * sin(rad)).roundToInt())),
                AndroidRobot.Swipe(1, src = src1, dest = centerPoint.offset((-toRadius * cos(rad)).roundToInt(), (-toRadius * sin(rad)).roundToInt()))
        ), ms)
        robot.lowLevelTouchActions {
            touchUp(0)
            touchUp(1)
        }
    }

    /**
     * Resets swipe delays to default.
     */
    @Synchronized
    private fun resetSwipeDelays() {
        Settings.DelayBeforeMouseDown = Settings.DelayValue
        Settings.DelayBeforeDrag = -Settings.DelayValue
        Settings.DelayBeforeDrop = Settings.DelayValue
    }

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
