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

import org.sikuli.script.*
import java.awt.Rectangle

/**
 * Interfaces that copies most of the [Region] APIs
 * Mainly used for allowing easy delegation of [AndroidMatch] functions to [AndroidRegion].
 * See [here](http://doc.sikuli.org/region.html) for more information.
 */
interface ISikuliRegion {
    /**
     * Set location of this region.
     *
     * @param loc New location to set to.
     * @return Region with the new location
     */
    fun setLocation(loc: Location): AndroidRegion

    /**
     * Sets the Region of Interest, mainly used to speed up searches.
     */
    fun setROI()

    /**
     * Sets the Region of Interest, mainly used to speed up searches.
     *
     * @param rect Sets the ROI to this rectangle.
     */
    fun setROI(rect: Rectangle)

    /**
     * Sets the Region of Interest, mainly used to speed up searches.
     *
     * @param region Sets the ROI to this region.
     */
    fun setROI(region: Region)

    /**
     * Sets the Region of Interest, mainly used to speed up searches.
     *
     * @param X x coordinate of top left corner of ROI
     * @param Y Y coordinate of top left corner of ROI
     * @param W Width of ROI
     * @param H Height of ROI
     */
    fun setROI(X: Int, Y: Int, W: Int, H: Int)

    /**
     * Gets the center of the region.
     *
     * @return Center point.
     */
    fun getCenter(): Location

    /**
     * Gets the top left location of the region.
     *
     * @return Top left point.
     */
    fun getTopLeft(): Location

    /**
     * Gets the top right location of the region.
     *
     * @return Top right point.
     */
    fun getTopRight(): Location

    /**
     * Gets the bottom left location of the region.
     *
     * @return Bottom left point.
     */
    fun getBottomLeft(): Location

    /**
     * Gets the bottom right location of the region.
     *
     * @return Bottom right point.
     */
    fun getBottomRight(): Location

    /**
     * Gets the last match matched in this region.
     *
     * @return Last match if found else null.
     */
    fun getLastMatch(): AndroidMatch?

    /**
     * Gets last matches matched in this region.
     *
     * @return [Iterator] of matches. May be empty.
     */
    fun getLastMatches(): Iterator<AndroidMatch>

    /**
     * Returns a new Region object, whose upper left corner is relocated adding the location’s x
     * and y value to the respective values of the given region. Width and height are the same.
     * So this clones a region at a different place.
     *
     * @param loc Location object to offset by
     * @return Region with offset coordinates.
     */
    fun offset(loc: Location): AndroidRegion

    /**
     * Creates a region grown to the given values in pixels. Negative values can be passed to
     * shrink instead.
     *
     * @param l Amount to grow on left side.
     * @param r Amount to grow on right side.
     * @param t Amount to grow on top side.
     * @param b Amount to grow on bottom side.
     * @return Grown region.
     */
    fun grow(l: Int, r: Int, t: Int, b: Int): AndroidRegion

    /**
     * Creates a region grown to the given values in pixels. Negative values can be passed to
     * shrink instead.
     *
     * @param w Amount to grow horizontally.
     * @param h Amount to grow vertically.
     * @return Grown region.
     */
    fun grow(w: Int, h: Int): AndroidRegion

    /**
     * Creates a region grown to the given values in pixels. Negative values can be passed to
     * shrink instead.
     *
     * @param range Amount to grow on each side.
     * @return Grown region.
     */
    fun grow(range: Int): AndroidRegion

    /**
     * Creates a region grown in pixels specified by [org.sikuli.basics.Settings.DefaultPadding].
     *
     * @return Grown region.
     */
    fun grow(): AndroidRegion

    /**
     * Creates a new region containing both regions.
     *
     * @param region Region to unite with.
     * @return The combined region.
     */
    fun union(region: Region): AndroidRegion

    /**
     * Creates a region that is the intersection of the given regions.
     *
     * @param region The region to intersect with.
     * @return The intersected region.
     */
    fun intersection(region: Region): AndroidRegion

    /**
     * Gets the region above the top side with same width, the new region extends to the top screen
     * border.
     *
     * @return The above region.
     */
    fun above(): AndroidRegion

    /**
     * Gets the region below the bottom side with same width, the new region extends to the bottom
     * screen border.
     *
     * @return The below region.
     */
    fun below(): AndroidRegion

    /**
     * Gets the region left to the left side with same height, the new region extends to the left
     * screen border.
     *
     * @return The left region.
     */
    fun left(): AndroidRegion

    /**
     * Gets the region right to the right side with same height, the new region extends to the right
     * screen border.
     *
     * @return The right region.
     */
    fun right(): AndroidRegion

    /**
     * Gets the region above the top side with same width, the new region extends by `height` pixels.
     *
     * @param height Height of the new region.
     * @return The above region.
     */
    fun above(height: Int): AndroidRegion

    /**
     * Gets the region below the bottom side with same width, the new region extends by `height` pixels.
     *
     * @param height Height of the new region.
     * @return The below region.
     */
    fun below(height: Int): AndroidRegion

    /**
     * Gets the region left to the left side with same height, the new region extends by `width` pixels.
     *
     * @param width Width of the new region.
     * @return The left region.
     */
    fun left(width: Int): AndroidRegion

    /**
     * Gets the region right to the right side with same height, the new region extends by `width` pixels.
     *
     * @param width Width of the new region.
     * @return The right region.
     */
    fun right(width: Int): AndroidRegion

    //<editor-fold desc="Search operations">

    /**
     * Finds the given target in the region and returns the best match.
     * If AutoWaitTimeout is set, this is equivalent to wait(). Otherwise only one search attempt
     * will be done.
     *
     * @param PSI [Pattern], [String] or [Image].
     * @param target A search criteria.
     * @return Found element.
     * @throws FindFailed If the find operation failed.
     */
    fun <PSI : Any> find(target: PSI): AndroidMatch

    /**
     * Finds all occurrences of the given target in the region and returns an
     * [Iterator] of [AndroidMatch].
     *
     * @param PSI [Pattern], [String] or [Image].
     * @param target A search criteria.
     * @return [Iterator] of matches.
     * @throws FindFailed If the find operation failed.
     */
    fun <PSI : Any> findAll(target: PSI): Iterator<AndroidMatch>

    /**
     * Waits for the target to appear until the AutoWaitTimeout value is exceeded.
     *
     * @param PSI [Pattern], [String] or [Image].
     * @param target The target to search for.
     * @return The found match.
     * @throws FindFailed If the find operation finally failed.
     */
    fun <PSI : Any> wait(target: PSI): AndroidMatch

    /**
     * Waits for the target to appear or timeout (in second) is passed.
     *
     * @param PSI [Pattern], [String] or [Image].
     * @param target The target to search for.
     * @param timeout Timeout in seconds.
     * @return The found match.
     * @throws FindFailed If the find operation finally failed.
     */
    fun <PSI : Any> wait(target: PSI, timeout: Double): AndroidMatch

    /**
     * Check if target exists (with the default autoWaitTimeout)
     *
     * @param PSI [Pattern], [String] or [Image].
     * @param target The target to check for.
     * @return The found match, null if not found
     */
    fun <PSI : Any> exists(target: PSI): AndroidMatch?

    /**
     * Check if target exists (with the default autoWaitTimeout)
     *
     * @param PSI [Pattern], [String] or [Image].
     * @param target The target to check for.
     * @return The found match, null if not found
     */
    fun <PSI : Any> exists(target: PSI, timeout: Double): AndroidMatch?

    //</editor-fold>

    //<editor-fold desc="Mouse and Keyboard Actions">
    /**
     * Left tap at the region's last successful match, uses center if there is no last match.
     * If region is a match, tap targetOffset.
     *
     * @return 1 if possible, 0 otherwise.
     */
    fun click(): Int

    /**
     * Attempts to find the target and tap it.
     *
     * @param target Target to tap.
     * @return 1 if possible, 0 otherwise.
     */
    fun <PFRML : Any> click(target: PFRML): Int

    /**
     * Attempts to find the target and tap it with modifiers.
     *
     * @param target Target to tap.
     * @param modifiers Modifiers to press while clicking.
     * @return 1 if possible, 0 otherwise.
     */
    fun <PFRML : Any> click(target: PFRML, modifiers: Int): Int

    /**
     * Like [click] but double that.
     */
    fun doubleClick(): Int

    /**
     * Like [click] but double that.
     *
     * @param target Target to tap.
     * @return 1 if possible, 0 otherwise.
     */
    fun <PFRML : Any> doubleClick(target: PFRML): Int

    /**
     * Like [click] but double that.
     *
     * @param target Target to tap.
     * @param modifiers Modifiers to press while clicking.
     * @return 1 if possible, 0 otherwise.
     */
    fun <PFRML : Any> doubleClick(target: PFRML, modifiers: Int): Int

    /**
     * Like [click] but uses right mouse button instead.
     */
    fun rightClick(): Int

    /**
     * Like [click] but uses right mouse button instead.
     *
     * @param target Target to tap.
     * @return 1 if possible, 0 otherwise.
     */
    fun <PFRML : Any> rightClick(target: PFRML): Int

    /**
     * Like [click] but uses right mouse button instead.
     *
     * @param target Target to tap.
     * @param modifiers Modifiers to press while clicking.
     * @return 1 if possible, 0 otherwise.
     */
    fun <PFRML : Any> rightClick(target: PFRML, modifiers: Int): Int

    /**
     * Move the mouse pointer to region's last successful match, uses center if no last match.
     * If region is a match, move to targetOffset. Same as [mouseMove].
     *
     * @return 1 if possible, 0 otherwise.
     */
    fun hover(): Int

    /**
     * Attempts to find the target and move the mouse pointer to it.
     *
     * @param target Target to move the pointer to.
     * @return 1 if possible, 0 otherwise.
     */
    fun <PFRML : Any> hover(target: PFRML): Int

    /**
     * Drags from region's last match and drop at given target using left mouse button.
     *
     * @param PFRML [Pattern], Filename ([String]), [Region], [Match] or [Location].
     * @param target Target to drop at.
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> dragDrop(target: PFRML): Int

    /**
     * Drag from a position and drop to another using left mouse button.
     *
     * @param PFRML [Pattern], Filename ([String]), [Region], [Match] or [Location].
     * @param t1 Source position.
     * @param t2 Target position.
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> dragDrop(t1: PFRML, t2: PFRML): Int

    /**
     * Initiates a mouse startSwipe action. (Moves mouse to a location and presses without releasing)
     *
     * @param PFRML [Pattern], Filename ([String]), [Region], [Match] or [Location].
     * @param target Location to start the mouse startSwipe action
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> drag(target: PFRML): Int

    /**
     * Ends a mouse drop action. (Moves mouse to a location and releases the button)
     *
     * @param PFRML [Pattern], Filename ([String]), [Region], [Match] or [Location].
     * @param target Location to release the mouse button.
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> dropAt(target: PFRML): Int

    /**
     * Types the given text into the current caret position. Only ASCII characters are supported.
     *
     * @param text Text to type.
     * @return 1 if possible, 0 otherwise.
     */
    fun type(text: String): Int

    /**
     * Types the given text into the current caret position. Only ASCII characters are supported.
     *
     * @param text Text to type.
     * @param modifiers Modifiers to press while typing.
     * @return 1 if possible, 0 otherwise.
     */
    fun type(text: String, modifiers: String): Int

    /**
     * Types the given text into the current caret position. Only ASCII characters are supported.
     *
     * @param text Text to type.
     * @param modifiers Modifiers to press while typing.
     * @return 1 if possible, 0 otherwise.
     */
    fun type(text: String, modifiers: Int): Int

    /**
     * Types the given text into the target. Only ASCII characters are supported.
     *
     * @param target Target to type into
     * @param text Text to type.
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> type(target: PFRML, text: String): Int

    /**
     * Types the given text into the target. Only ASCII characters are supported.
     *
     * @param target Target to type into
     * @param text Text to type.
     * @param modifiers Modifiers to press while typing.
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> type(target: PFRML, text: String, modifiers: String): Int

    /**
     * Types the given text into the target. Only ASCII characters are supported.
     *
     * @param target Target to type into
     * @param text Text to type.
     * @param modifiers Modifiers to press while typing.
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> type(target: PFRML, text: String, modifiers: Int): Int

    /**
     * Pastes the given text into the current caret position.
     *
     * @param text Text to paste.
     * @return 1 if possible, 0 otherwise.
     */
    fun paste(text: String): Int

    /**
     * Pastes the given text into the target.
     *
     * @param target Target to paste into
     * @param text Text to paste.
     * @return 1 if possible, 0 otherwise.
     */
    fun <PFRML : Any> paste(target: PFRML, text: String): Int
    //</editor-fold>

    //<editor-fold desc="Low-level Mouse and Keyboard Actions">
    /**
     * Presses mouse buttons.
     *
     * @param buttons The mouse buttons to press.
     */
    fun mouseDown(buttons: Int)

    /**
     * Releases all mouse buttons.
     */
    fun mouseUp()

    /**
     * Releases mouse buttons.
     *
     * @param buttons The mouse buttons to release.
     */
    fun mouseUp(buttons: Int)

    /**
     * Moves the mouse pointer to the region's last successful match. Same as [hover].
     *
     * @return 1 if possible, 0 otherwise
     */
    fun mouseMove(): Int

    /**
     * Moves the mouse from the current position to the offset position.
     *
     * @param xoff Horizontal offset, negative number are offset to the left.
     * @param yoff Vertical offset, negative number are offset to the top.
     * @return 1 if possible, 0 otherwise.
     */
    fun mouseMove(xoff: Int, yoff: Int): Int

    /**
     * Moves the mouse to the given target.
     *
     * @param target Target to move the pointer to.
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> mouseMove(target: PFRML): Int

    /**
     * Spins the mouse wheel.
     *
     * @param direction Direction to spin the mouse wheel in.
     * @param steps Number of steps the wheel is spinned.
     * @return 1 if possible, 0 otherwise.
     */
    fun wheel(direction: Int, steps: Int): Int

    /**
     * Spins the mouse wheel at the given target
     *
     * @param target Target to spin the wheel at.
     * @param direction Direction to spin the mouse wheel in.
     * @param steps Number of steps the wheel is spinned.
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> wheel(target: PFRML, direction: Int, steps: Int): Int

    /**
     * Spins the mouse wheel at the given target
     *
     * @param target Target to spin the wheel at.
     * @param direction Direction to spin the mouse wheel in.
     * @param steps Number of steps the wheel is spinned.
     * @param stepDelay The delay in milliseconds between each wheel step.
     * @return 1 if possible, 0 otherwise.
     * @throws FindFailed If the find operation failed.
     */
    fun <PFRML : Any> wheel(target: PFRML, direction: Int, steps: Int, stepDelay: Int): Int

    /**
     * Presses a specific key.
     *
     * @param keycode The key to press.
     */
    fun keyDown(keycode: Int)

    /**
     * Presses the keys specified by the string.
     *
     * @param keys Keys to be pressed.
     */
    fun keyDown(keys: String)

    /**
     * Releases all keys
     */
    fun keyUp()

    /**
     * Releases a specific key.
     *
     * @param keycode The key to release.
     */
    fun keyUp(keycode: Int)

    /**
     * Releases the keys specified by the string.
     *
     * @param keys Keys to be released.
     */
    fun keyUp(keys: String)
    //</editor-fold>

    //<editor-fold desc="Highlight Action">
    /**
     * Highlights the region.
     *
     * @return Highlighted region.
     */
    fun highlight(): AndroidRegion

    /**
     * Highlights the region with the given color
     *
     * @param color Color to highlight with, can be color name or hex color codes.
     * @return Highlighted region.
     */
    fun highlight(color: String): AndroidRegion

    /**
     * Highlights the region for the given duration.
     *
     * @param secs Duration to highlight in seconds.
     * @return Highlighted region.
     */
    fun highlight(secs: Int): AndroidRegion

    /**
     * Highlights the region for the given duration.
     *
     * @param secs Duration to highlight in seconds.
     * @return Highlighted region.
     */
    fun highlight(secs: Float): AndroidRegion

    /**
     * Highlights the region for the given duration and color.
     *
     * @param secs Duration to highlight in seconds.
     * @param color Color to highlight with, can be color name or hex color codes.
     * @return Highlighted region.
     */
    fun highlight(secs: Int, color: String): AndroidRegion

    /**
     * Highlights the region for the given duration and color.
     *
     * @param secs Duration to highlight in seconds.
     * @param color Color to highlight with, can be color name or hex color codes.
     * @return Highlighted region.
     */
    fun highlight(secs: Float, color: String): AndroidRegion

    //</editor-fold>

    /**
     * Gets the [Location] of the target.
     *
     * @param PSIMRL [Pattern], [String], [Image], [Match], [Region] or [Location].
     * @param target Target to find.
     * @return Location of target.
     * @throws FindFailed If the find operation failed.
     */
    fun <PSIMRL : Any> getLocationFromTarget(target: PSIMRL): Location

    //<editor-fold desc="Kotlin operator overloads">

    /**
     * Same as [union].
     *
     * @return Combined region.
     */
    operator fun plus(region: Region): AndroidRegion

    /**
     * Compares the regions in terms of area.
     *
     * @return Positive number if this region is larger, 0 if equal, negative if smaller.
     */
    operator fun compareTo(region: Region): Int

    //</editor-fold>
}
