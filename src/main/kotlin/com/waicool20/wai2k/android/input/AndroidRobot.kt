package com.waicool20.wai2k.android.input

import com.waicool20.wai2k.android.AndroidScreen
import com.waicool20.wai2k.android.enums.InputEvent
import com.waicool20.wai2k.android.enums.EventType
import org.sikuli.basics.AnimatorOutQuarticEase
import org.sikuli.basics.AnimatorTimeBased
import org.sikuli.basics.Settings
import org.sikuli.script.*
import java.awt.Color
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * A class representing a [org.sikuli.script.IRobot] that can be used to control a [AndroidScreen],
 * not recommended for normal use as this is not thread safe, any actions generated will be
 * unpredictable behaviour. It is recommended to use [AndroidKeyboard] and [AndroidTouchInterface] as
 * a thread safe wrapper to control this robot.
 *
 * @property screen [AndroidScreen] that is bound to this robot.
 * @constructor Main constructor
 * @param screen [AndroidScreen] to use this robot on.
 */
class AndroidRobot(val screen: AndroidScreen) : IRobot {
    private var autoDelay = 100
    private val mouseDown = AtomicBoolean(false)
    private val heldKeys = mutableSetOf<Int>()
    var cursorX: Int = 0
        private set
    var cursorY: Int = 0
        private set

    init {
        // Keep track of the x y coordinates by monitoring the input event bus
        thread(isDaemon = true) {
            screen.device.execute("getevent ${screen.device.input.devFile}").bufferedReader().forEachLine {
                val (_, sCode, sValue) = it.trim().split(Regex("\\s+"))
                val eventCode = sCode.toLong(16)
                val value = sValue.toLong(16)
                when (eventCode) {
                    InputEvent.ABS_MT_POSITION_X.code -> {
                        cursorX = valueToCoord(value, InputEvent.ABS_MT_POSITION_X)
                    }
                    InputEvent.ABS_MT_POSITION_Y.code -> {
                        cursorY = valueToCoord(value, InputEvent.ABS_MT_POSITION_Y)
                    }
                }
            }
        }
    }

    //<editor-fold desc="Mouse stuff">

    /**
     * Releases a touch
     *
     * @param buttons Mouse buttons to release. Ignored
     * @return The currently held buttons. Always 0
     */
    override fun mouseUp(buttons: Int): Int {
        if (mouseDown.compareAndSet(true, false)) {
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_PRESSURE, 0)
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_TRACKING_ID, 0xffffffff)
            syncEvent()
        }
        return 0
    }

    /**
     * Starts a touch action
     *
     * @param buttons Mouse buttons to press. Ignored
     */
    override fun mouseDown(buttons: Int) {
        if (mouseDown.compareAndSet(false, true)) {
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_TRACKING_ID, 0)
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_TOUCH_MAJOR, 127)
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_PRESSURE, 127)
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_POSITION_X, coordToValue(cursorX, InputEvent.ABS_MT_POSITION_X))
            sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_POSITION_Y, coordToValue(cursorY, InputEvent.ABS_MT_POSITION_Y))
            syncEvent()
        }
    }

    /**
     * Moves the cursor to the coordinates instantly.
     *
     * @param x x coordinate to move the cursor to.
     * @param y y coordinate to move the cursor to.
     */
    override fun mouseMove(x: Int, y: Int) {
        val xChanged = cursorX != x
        val yChanged = cursorY != y
        cursorX = if (xChanged) x else cursorX
        cursorY = if (yChanged) y else cursorY
        if (mouseDown.get()) {
            if (xChanged) sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_POSITION_X, coordToValue(x, InputEvent.ABS_MT_POSITION_X))
            if (yChanged) sendEvent(EventType.EV_ABS, InputEvent.ABS_MT_POSITION_Y, coordToValue(y, InputEvent.ABS_MT_POSITION_Y))
            if (xChanged || yChanged) syncEvent()
        }
    }

    /**
     * Spins the mouse wheel.
     *
     * @param wheelAmt Amount of steps to spin the wheel, can be negative to change direction.
     */
    override fun mouseWheel(wheelAmt: Int) {
        // TODO Pinch in?
    }

    /**
     * Moves the cursor to the destination in a smooth fashion.
     *
     * @param dest Location to move the cursor to.
     */
    override fun smoothMove(dest: Location) =
            smoothMove(Location(cursorX, cursorY), dest, (Settings.MoveMouseDelay * 1000).toLong())

    /**
     * Moves the cursor from a source to the destination in a smooth fashion.
     *
     * @param src Location to move the cursor from
     * @param dest Location to move the cursor to.
     * @param ms Length of time to complete this action within in milliseconds.
     */
    override fun smoothMove(src: Location, dest: Location, ms: Long) {
        if (ms < 1) {
            mouseMove(dest.x, dest.y)
            return
        }
        val aniX = AnimatorTimeBased(AnimatorOutQuarticEase(src.x.toFloat(), dest.x.toFloat(), ms))
        val aniY = AnimatorTimeBased(AnimatorOutQuarticEase(src.y.toFloat(), dest.y.toFloat(), ms))
        while (aniX.running()) {
            val x = aniX.step()
            val y = aniY.step()
            mouseMove(x.roundToInt(), y.roundToInt())
            delay(10)
        }
    }

    /**
     * Resets the cursor state. Mouse is moved to (0, 0) and all buttons are released.
     */
    override fun mouseReset() {
        mouseMove(0, 0)
        mouseUp(0)
    }

    //</editor-fold>

    //<editor-fold desc="Keyboard stuff">

    /**
     * Releases all keys.
     */
    override fun keyUp() = heldKeys.forEach { keyUp(it) }

    /**
     * Releases key with the given code.
     *
     * @param code Key to release.
     */
    override fun keyUp(code: Int) {
        //TODO("Keyboard input comes later")
    }

    /**
     * Releases keys specified in a string
     *
     * @param keys Keys to release.
     */
    override fun keyUp(keys: String) = keys.toCharArray().forEach {
        //TODO("Keyboard input comes later")
    }

    /**
     * Presses key with the given code.
     *
     * @param code Key to press.
     */
    override fun keyDown(code: Int) {
        //TODO("Keyboard input comes later")
    }

    /**
     * Presses keys specified in a string.
     *
     * @param keys Keys to press.
     */
    override fun keyDown(keys: String) = keys.toCharArray().forEach {
        //TODO("Keyboard input comes later")
    }

    /**
     * Types a given character.
     *
     * @param character Character to type.
     * @param mode Mode to type the character with.
     */
    override fun typeChar(character: Char, mode: IRobot.KeyMode) = when (mode) {
        IRobot.KeyMode.PRESS_ONLY -> {
            //TODO("Keyboard input comes later")
        }
        IRobot.KeyMode.PRESS_RELEASE -> {
            //TODO("Keyboard input comes later")
        }
        IRobot.KeyMode.RELEASE_ONLY -> {
            //TODO("Keyboard input comes later")
        }
    }

    /**
     * Types a given key.
     *
     * @param key Key to type.
     */
    override fun typeKey(key: Int) {
        //TODO("Keyboard input comes later")
        keyUp(key)
        keyDown(key)
    }

    /**
     * Presses key modifiers.
     *
     * @param modifiers The key modifiers to press.
     */
    override fun pressModifiers(modifiers: Int) {
        //TODO("Keyboard input comes later")
        if (modifiers and KeyModifier.SHIFT != 0) keyDown(KeyEvent.VK_SHIFT)
        if (modifiers and KeyModifier.CTRL != 0) keyDown(KeyEvent.VK_CONTROL)
        if (modifiers and KeyModifier.ALT != 0) keyDown(KeyEvent.VK_ALT)
        if (modifiers and KeyModifier.META != 0 || modifiers and KeyModifier.WIN != 0) {
            if (Settings.isWindows()) {
                keyDown(KeyEvent.VK_WINDOWS)
            } else {
                keyDown(KeyEvent.VK_META)
            }
        }
    }

    /**
     * Releases key modifiers.
     *
     * @param modifiers The key modifiers to release.
     */
    override fun releaseModifiers(modifiers: Int) {
        //TODO("Keyboard input comes later")
        if (modifiers and KeyModifier.SHIFT != 0) keyUp(KeyEvent.VK_SHIFT)
        if (modifiers and KeyModifier.CTRL != 0) keyUp(KeyEvent.VK_CONTROL)
        if (modifiers and KeyModifier.ALT != 0) keyUp(KeyEvent.VK_ALT)
        if (modifiers and KeyModifier.META != 0 || modifiers and KeyModifier.WIN != 0) {
            if (Settings.isWindows()) {
                keyUp(KeyEvent.VK_WINDOWS)
            } else {
                keyUp(KeyEvent.VK_META)
            }
        }
    }

    //</editor-fold>

    //<editor-fold desc="Misc">

    /**
     * Sets the auto delay for this robot.
     */
    override fun setAutoDelay(ms: Int) {
        autoDelay = ms
    }

    /**
     * Returns whether or not the robot is controlling a remote screen. Always true.
     */
    override fun isRemote() = true

    /**
     * Sleeps the current thread for a duration
     *
     * @param ms Time to sleep in milliseconds.
     */
    override fun delay(ms: Int) = TimeUnit.MILLISECONDS.sleep(ms.toLong())

    //</editor-fold>

    //<editor-fold desc="Screen Stuff">

    /**
     * Takes a screenshot of the screen bound to this robot
     *
     * @param screenRect Sub-region to capture.
     * @return the captured image.
     */
    override fun captureScreen(screenRect: Rectangle): ScreenImage = screen.capture(screenRect)

    /**
     * Gets the bound screen instance.
     */
    override fun getScreen(): IScreen = screen

    /**
     * Gets the color of the pixel at coordinates.
     *
     * @param x x coordinate of the pixel.
     * @param y y coordinate of the pixel.
     */
    override fun getColorAt(x: Int, y: Int): Color = Color((screen.lastScreenImage
            ?: screen.capture()).image.getRGB(x, y))

    //</editor-fold>

    //<editor-fold desc="Stuff that does Nothing">

    /**
     * Does nothing, ignore.
     */
    override fun cleanup() = Unit

    /**
     * Does nothing, ignore.
     */
    override fun clickStarts() = Unit

    /**
     * Does nothing, ignore.
     */
    override fun clickEnds() = Unit

    /**
     * Does nothing, ignore.
     */
    override fun typeStarts() = Unit

    /**
     * Does nothing, ignore.
     */
    override fun typeEnds() = Unit

    /**
     * Does nothing, ignore.
     */
    override fun waitForIdle() = Unit

    //</editor-fold>

    private fun sendEvent(type: EventType, code: InputEvent, value: Long) {
        screen.device.execute("sendevent ${screen.device.input.devFile} ${type.code} ${code.code} $value")
    }

    private fun syncEvent() = sendEvent(EventType.EV_SYN, InputEvent.SYN_REPORT, 0)

    private fun valueToCoord(value: Long, code: InputEvent): Int {
        return when (code) {
            InputEvent.ABS_MT_POSITION_X -> {
                val max = screen.device.input.specs[code]?.maxValue ?: 1
                ((value / max.toDouble()) * screen.device.properties.displayWidth).roundToInt()
            }
            InputEvent.ABS_MT_POSITION_Y -> {
                val max = screen.device.input.specs[code]?.maxValue ?: 1
                ((value / max.toDouble()) * screen.device.properties.displayHeight).roundToInt()
            }
            else -> error("Unsupported code")
        }
    }

    private fun coordToValue(coord: Int, code: InputEvent): Long {
        return when (code) {
            InputEvent.ABS_MT_POSITION_X -> {
                val max = screen.device.input.specs[code]?.maxValue ?: 1
                ((coord / screen.device.properties.displayWidth.toDouble()) * max).roundToInt()
            }
            InputEvent.ABS_MT_POSITION_Y -> {
                val max = screen.device.input.specs[code]?.maxValue ?: 1
                ((coord / screen.device.properties.displayHeight.toDouble()) * max).roundToInt()
            }
            else -> error("Unsupported code")
        }.toLong()
    }
}
