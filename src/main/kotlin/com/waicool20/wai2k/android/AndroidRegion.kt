package com.waicool20.wai2k.android

import com.waicool20.wai2k.android.input.AndroidKeyboard
import org.sikuli.script.*
import java.awt.Image
import java.awt.Rectangle
import java.awt.event.KeyEvent

open class AndroidRegion(xPos: Int, yPos: Int, width: Int, height: Int) : Region(), ISikuliRegion {
    /**
     * Region constructor
     *
     * @param region Region to inherit from.
     * @param screen Screen that this region should belong to.
     */
    constructor(region: Region, screen: AndroidScreen) : this(region.x, region.y, region.w, region.h, screen)

    /**
     * Rectangle constructor
     *
     * @param rect Uses the rectangles dimensions to construct this region.
     * @param screen Screen that this region should belong to.
     */
    constructor(rect: Rectangle, screen: AndroidScreen) : this(rect.x, rect.y, rect.width, rect.height, screen)

    /**
     * Like main constructor but has additional screen param.
     *
     * @param xPos x coordinate of the top left corner of the region.
     * @param yPos y coordinate of the top left corner of the region.
     * @param width Width of the region.
     * @param height Height of the region.
     * @param screen Screen that this region should belong to.
     */
    constructor(xPos: Int, yPos: Int, width: Int, height: Int, screen: AndroidScreen) : this(xPos, yPos, width, height) {
        this.screen = screen
    }

    init {
        x = xPos
        y = yPos
        w = width
        h = height
    }

    private val touchInterface by lazy { androidScreen().touchInterface }
    private val keyboard by lazy { androidScreen().keyboard }

    /**
     * Gets the screen this region belongs to as a [AndroidScreen]
     */
    fun androidScreen() = screen as AndroidScreen

    override fun setLocation(loc: Location): AndroidRegion {
        x = loc.x
        y = loc.y
        return this
    }

    override fun setROI() = setROI(screen.bounds)
    override fun setROI(rect: Rectangle) = with(rect) { setROI(x, y, width, height) }
    override fun setROI(region: Region) = with(region) { setROI(x, y, w, h) }
    override fun setROI(X: Int, Y: Int, W: Int, H: Int) {
        x = X
        y = Y
        w = if (W > 1) W else 1
        h = if (H > 1) H else 1
    }

    override fun getCenter() = Location(x + (w / 2), y + (h / 2))
    override fun getTopLeft() = Location(x, y)
    override fun getTopRight() = Location(x + w - 1, y)
    override fun getBottomLeft() = Location(x, y + h - 1)
    override fun getBottomRight() = Location(x + w - 1, y + h - 1)

    override fun getLastMatch() = super.getLastMatch()?.let { AndroidMatch(it, androidScreen()) }
    override fun getLastMatches() =
            super.getLastMatches().asSequence().map { AndroidMatch(it, androidScreen()) }.iterator()

    override fun offset(loc: Location) = AndroidRegion(super.offset(loc), androidScreen())
    override fun grow(l: Int, r: Int, t: Int, b: Int) = AndroidRegion(super.grow(l, r, t, b), androidScreen())
    override fun grow(w: Int, h: Int) = AndroidRegion(super.grow(w, h), androidScreen())
    override fun grow(range: Int) = AndroidRegion(super.grow(range), androidScreen())
    override fun grow() = AndroidRegion(super.grow(), androidScreen())

    override fun union(region: Region) = AndroidRegion(super.union(region), androidScreen())
    override fun intersection(region: Region) = AndroidRegion(super.intersection(region), androidScreen())

    override fun above() = AndroidRegion(super.above(), androidScreen())
    override fun below() = AndroidRegion(super.below(), androidScreen())
    override fun left() = AndroidRegion(super.left(), androidScreen())
    override fun right() = AndroidRegion(super.right(), androidScreen())

    override fun above(height: Int) = AndroidRegion(super.above(height), androidScreen())
    override fun below(height: Int) = AndroidRegion(super.below(height), androidScreen())
    override fun left(width: Int) = AndroidRegion(super.left(width), androidScreen())
    override fun right(width: Int) = AndroidRegion(super.right(width), androidScreen())

    //<editor-fold desc="Search operations">

    override fun <PSI : Any> find(target: PSI) = AndroidMatch(super.find(target), androidScreen())
    override fun <PSI : Any> findAll(target: PSI) =
            super.findAll(target).asSequence().map { AndroidMatch(it, androidScreen()) }.iterator()

    override fun <PSI : Any> wait(target: PSI) = AndroidMatch(super.wait(target), androidScreen())
    override fun <PSI : Any> wait(target: PSI, timeout: Double) = AndroidMatch(super.wait(target, timeout), androidScreen())

    override fun <PSI : Any> exists(target: PSI) = super.exists(target)?.let { AndroidMatch(it, androidScreen()) }
    override fun <PSI : Any> exists(target: PSI, timeout: Double) = super.exists(target, timeout)?.let { AndroidMatch(it, androidScreen()) }

    //</editor-fold>

    //<editor-fold desc="Mouse and Keyboard Actions">
    override fun click(): Int = click(center, 0)

    override fun <PFRML : Any> click(target: PFRML): Int = click(target, 0)
    override fun <PFRML : Any> click(target: PFRML, modifiers: Int): Int = try {
        touchInterface.tap(getLocationFromTarget(target), modifiers)
        1
    } catch (e: FindFailed) {
        0
    }

    override fun doubleClick(): Int = doubleClick(center, 0)
    override fun <PFRML : Any> doubleClick(target: PFRML): Int = doubleClick(target, 0)
    override fun <PFRML : Any> doubleClick(target: PFRML, modifiers: Int): Int = try {
        touchInterface.doubleTap(getLocationFromTarget(target), modifiers)
        1
    } catch (e: FindFailed) {
        0
    }

    override fun rightClick(): Int = rightClick(center, 0)
    override fun <PFRML : Any> rightClick(target: PFRML): Int = rightClick(target, 0)
    override fun <PFRML : Any> rightClick(target: PFRML, modifiers: Int): Int = try {
        touchInterface.tap(getLocationFromTarget(target), modifiers)
        1
    } catch (e: FindFailed) {
        0
    }

    override fun hover(): Int = hover(center)
    override fun <PFRML : Any> hover(target: PFRML): Int = try {
        touchInterface.moveTo(getLocationFromTarget(target))
        1
    } catch (e: FindFailed) {
        0
    }

    override fun <PFRML : Any> dragDrop(target: PFRML): Int = lastMatch?.let { dragDrop(it, target) }
            ?: 0

    override fun <PFRML : Any> dragDrop(t1: PFRML, t2: PFRML): Int = try {
        touchInterface.swipe(getLocationFromTarget(t1), getLocationFromTarget(t2))
        1
    } catch (e: FindFailed) {
        0
    }

    override fun <PFRML : Any> drag(target: PFRML): Int = try {
        touchInterface.startSwipe(getLocationFromTarget(target))
        1
    } catch (e: FindFailed) {
        0
    }

    override fun <PFRML : Any> dropAt(target: PFRML): Int = try {
        touchInterface.endSwipe(getLocationFromTarget(target))
        1
    } catch (e: FindFailed) {
        0
    }

    override fun type(text: String): Int = type(text, 0)
    override fun type(text: String, modifiers: String): Int = type(text, AndroidKeyboard.parseModifiers(modifiers))
    override fun type(text: String, modifiers: Int): Int {
        keyboard.type(null, text, modifiers)
        return 1
    }

    override fun <PFRML : Any> type(target: PFRML, text: String): Int = type(target, text, 0)
    override fun <PFRML : Any> type(target: PFRML, text: String, modifiers: String): Int =
            type(target, text, AndroidKeyboard.parseModifiers(modifiers))

    override fun <PFRML : Any> type(target: PFRML, text: String, modifiers: Int): Int = try {
        keyboard.type(getLocationFromTarget(target), text, modifiers)
        1
    } catch (e: FindFailed) {
        0
    }

    override fun paste(text: String): Int {
        androidScreen().clipboard = text
        keyboard.atomicAction {
            keyDown(Key.getHotkeyModifier())
            keyDown(KeyEvent.VK_V)
            keyUp(KeyEvent.VK_V)
            keyUp(Key.getHotkeyModifier())
        }
        return 0
    }

    override fun <PFRML : Any> paste(target: PFRML, text: String): Int {
        if (text.isEmpty() || click(target) == 1) return 1
        return paste(text)
    }
    //</editor-fold>

    //<editor-fold desc="Low-level Mouse and Keyboard Actions">
    override fun mouseDown(buttons: Int) = touchInterface.pressDown()

    override fun mouseUp() {
        touchInterface.pressUp()
    }

    override fun mouseUp(buttons: Int) {
        touchInterface.pressUp()
    }

    override fun mouseMove(): Int = lastMatch?.let { mouseMove(it) } ?: 0
    override fun mouseMove(xoff: Int, yoff: Int): Int = mouseMove(Location(x + xoff, y + yoff))
    override fun <PFRML : Any> mouseMove(target: PFRML): Int = try {
        touchInterface.moveTo(getLocationFromTarget(target))
        1
    } catch (e: FindFailed) {
        0
    }

    override fun wheel(direction: Int, steps: Int): Int = wheel(androidScreen().currentMousePosition(), direction, steps)
    override fun <PFRML : Any> wheel(target: PFRML, direction: Int, steps: Int): Int = wheel(target, direction, steps, Mouse.WHEEL_STEP_DELAY)
    override fun <PFRML : Any> wheel(target: PFRML, direction: Int, steps: Int, stepDelay: Int): Int {
        touchInterface.spinWheel(getLocationFromTarget(target), direction, steps, stepDelay)
        return 1
    }

    override fun keyDown(keycode: Int) = keyboard.keyDown(keycode)
    override fun keyDown(keys: String) = keyboard.keyDown(keys)

    override fun keyUp() = keyboard.keyUp()
    override fun keyUp(keycode: Int) = keyboard.keyUp(keycode)
    override fun keyUp(keys: String) = keyboard.keyUp(keys)
    //</editor-fold>

    //<editor-fold desc="Highlight Action">
    override fun highlight() = highlight("")

    override fun highlight(color: String): AndroidRegion {
        throw UnsupportedOperationException()
    }

    override fun highlight(secs: Int) = highlight(secs, "")
    override fun highlight(secs: Float) = highlight(secs, "")
    override fun highlight(secs: Int, color: String) = highlight(secs.toFloat(), color)

    override fun highlight(secs: Float, color: String): AndroidRegion {
        throw UnsupportedOperationException()
    }

    //</editor-fold>

    override fun <PSIMRL : Any> getLocationFromTarget(target: PSIMRL): Location = when (target) {
        is Pattern, is String, is Image -> find(target).target
        is Match -> target.target
        is AndroidRegion -> target.center
        is Region -> target.center
        is Location -> target
        else -> throw FindFailed("Not able to get location from $target")
    }.setOtherScreen(screen)

    //<editor-fold desc="Kotlin operator overloads">

    override operator fun plus(region: Region) = union(region)
    override operator fun compareTo(region: Region) = (w * h) - region.let { it.w * it.h }

    //</editor-fold>

    //<editor-fold desc="Unsupported"

    //</editor-fold>
}

