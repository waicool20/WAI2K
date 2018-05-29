package com.waicool20.wai2k.android.input


import com.waicool20.wai2k.android.AndroidScreen
import org.sikuli.basics.Settings
import org.sikuli.script.Location

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
    fun tap(location: Location, modifiers: Int) = synchronized(this) {
        moveTo(location)
        val pause = if (Settings.ClickDelay > 1) 1 else (Settings.ClickDelay * 1000).toInt()
        robot.pressModifiers(modifiers)
        robot.mouseDown(0)
        robot.delay(pause)
        robot.mouseUp(0)
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
    fun doubleTap(location: Location, modifiers: Int) = synchronized(this) {
        repeat(2) {
            tap(location, modifiers)
        }
    }

    /**
     * Moves the cursor to a given location.
     *
     * @param location The location to move the cursor to.
     */
    @Synchronized
    fun moveTo(location: Location) = synchronized(this) { robot.smoothMove(location) }

    /* Low level actions */

    /**
     * Presses the screen.
     *
     */
    @Synchronized
    fun pressDown() = synchronized(this) { robot.mouseDown(0) }

    /**
     * Releases the screen.
     *
     */
    @Synchronized
    fun pressUp(): Int = synchronized(this) { robot.mouseUp(0) }

    /**
     * Spins the mouse wheel.
     *
     * @param location Location to spin the mouse wheel at,
     * may be null to just spin the wheel directly.
     * @param direction Direction to spin the mouse wheel in.
     * @param steps Number of steps the wheel is spinned.
     * @param stepDelay The delay in milliseconds between each wheel step.
     */
    @Synchronized
    fun spinWheel(location: Location?, direction: Int, steps: Int, stepDelay: Int) = synchronized(this) {
        // TODO Pinch in?
        location?.let { moveTo(it) }
        repeat(steps) {
            robot.mouseWheel(if (direction < 0) -1 else 1)
            robot.delay(stepDelay)
        }
    }

    /**
     * Initiates a swipe action. (Moves cursor to a location and presses without releasing)
     *
     * @param location Location to start the swipe action
     * @param resetDelays Whether or not to reset delays after pressing.
     */
    @Synchronized
    fun startSwipe(location: Location, resetDelays: Boolean = true) = synchronized(this) {
        moveTo(location)
        robot.delay((Settings.DelayBeforeMouseDown * 1000).toInt())
        pressDown()
        robot.delay((if (Settings.DelayBeforeDrag < 0) Settings.DelayAfterDrag else Settings.DelayBeforeDrag).toInt() * 1000)
        if (resetDelays) resetSwipeDelays()
    }

    /**
     * Ends a swipe action. (Moves mouse to a location and releases the button)
     *
     * @param location Location to release the swipe.
     * @param resetDelays Whether or not to reset delays after releasing.
     */
    @Synchronized
    fun endSwipe(location: Location, resetDelays: Boolean = true) = synchronized(this) {
        moveTo(location)
        robot.delay((Settings.DelayBeforeDrop * 1000).toInt())
        pressUp()
        if (resetDelays) resetSwipeDelays()
    }

    /**
     * Same as calling [startSwipe] with [loc1] then [endSwipe] with [loc2] but does this all at once while
     * keeping the lock to this object.
     *
     * @param loc1 Location to begin startSwipe.
     * @param loc2 Location to drop at.
     */
    @Synchronized
    fun swipe(loc1: Location, loc2: Location) = synchronized(this) {
        startSwipe(loc1, false)
        endSwipe(loc2)
    }

    /**
     * Resets swipe delays to default.
     */
    @Synchronized
    private fun resetSwipeDelays() {
        Settings.DelayBeforeMouseDown = Settings.DelayValue
        Settings.DelayAfterDrag = Settings.DelayValue
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
