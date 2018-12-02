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

import com.waicool20.wai2k.android.input.AndroidKeyboard
import com.waicool20.wai2k.android.input.AndroidRobot
import com.waicool20.wai2k.android.input.AndroidTouchInterface
import org.sikuli.script.*
import java.awt.Rectangle

class AndroidScreen(val device: AndroidDevice) : AndroidRegion(
        xPos = 0,
        yPos = 0,
        width = device.properties.displayWidth,
        height = device.properties.displayHeight
), IScreen {
    private val robot = AndroidRobot(this)
    private var _lastScreenImage: ScreenImage? = null

    /**
     * Independent virtual touch interface bound to this screen.
     */
    val touchInterface = AndroidTouchInterface(robot)
    /**
     * Independent virtual keyboard bound to this screen.
     */
    val keyboard = AndroidKeyboard(robot)
    // TODO Use ADB to get and set android clipboard
    /**
     * Independent clipboard bound to this screen.
     */
    var clipboard = ""

    init {
        isVirtual = true
        setOtherScreen(this)
    }

    override fun showTarget(location: Location) {
        throw UnsupportedOperationException()
    }

    /**
     * Gets the ID of a given point. Always 0 for a [AndroidScreen]
     *
     * @param srcx x coordinate of point.
     * @param srcy y coordinate of point.
     */
    override fun getIdFromPoint(srcx: Int, srcy: Int): Int = 0

    /**
     * Gets the underlying robot instance used to control this screen.
     */
    override fun getRobot(): IRobot = robot

    override fun userCapture(string: String?): ScreenImage {
        throw UnsupportedOperationException()
    }

    /**
     * Returns the ID of the screen, returns a unique hash
     */
    override fun getID(): Int = device.adbSerial.hashCode()

    /**
     * Returns the geometry of the underlying android device.
     */
    override fun getBounds(): Rectangle = device.properties.let {
        Rectangle(it.displayWidth, it.displayHeight)
    }

    /**
     * Takes a screenshot of the whole screen.
     *
     * @return the captured image.
     */
    override fun capture(): ScreenImage = capture(rect)

    /**
     * Takes a screenshot of the screen.
     *
     * @param region Sub-region to capture.
     * @return the captured image.
     */
    override fun capture(region: Region): ScreenImage = capture(region.x, region.y, region.w, region.h)

    /**
     * Takes a screenshot of the screen.
     *
     * @param x x coordinate of the sub-region.
     * @param y y coordinate of the sub-region.
     * @param width width of the sub-region.
     * @param height height of the sub-region.
     * @return the captured image.
     */
    override fun capture(x: Int, y: Int, width: Int, height: Int): ScreenImage = capture(Rectangle(x, y, width, height))

    /**
     * Takes a screenshot of the screen.
     *
     * @param rect Sub-region to capture.
     * @return the captured image.
     */
    override fun capture(rect: Rectangle): ScreenImage = device.takeFastScreenshot().let {
        setW(it.width)
        setH(it.height)
        ScreenImage(Rectangle(0, 0, it.width, it.height), it).getSub(rect)
    }.also { _lastScreenImage = it }

    /**
     * Returns the last saved screen image.
     */
    override fun getLastScreenImageFromScreen(): ScreenImage? = _lastScreenImage

    /**
     * Creates a new location on this screen
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    override fun newLocation(x: Int, y: Int): Location = Location(x, y).setOtherScreen(this)

    /**
     * Creates a new location on this screen
     *
     * @param loc Location object to copy from
     */
    override fun newLocation(loc: Location): Location = Location(loc).setOtherScreen(this)

    /**
     * Creates a new region on this screen
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param w Width
     * @param h Height
     */
    override fun newRegion(x: Int, y: Int, w: Int, h: Int): AndroidRegion = AndroidRegion(x, y, w, h, this)

    /**
     * Creates a new region on this screen
     *
     * @param loc Location of this region
     * @param w Width
     * @param h Height
     */
    override fun newRegion(loc: Location, w: Int, h: Int): AndroidRegion = AndroidRegion(loc.x, loc.y, w, h, this)

    /**
     * Creates a new region on this screen
     *
     * @param reg Region object to copy from
     */
    override fun newRegion(reg: Region): AndroidRegion = AndroidRegion(reg, this)

    /**
     * Sets the regions screen to this screen
     *
     * @param element Region to set
     */
    override fun setOther(element: Region): AndroidRegion = newRegion(element).setOtherScreen(this)

    /**
     * Sets the locations screen to this screen
     *
     * @param element Location to set
     */
    override fun setOther(element: Location): Location = element.setOtherScreen(this)

    /**
     * Gets the current position of the virtual mouse.
     */
    fun currentMousePosition() = Location(robot.touches[0].cursorX, robot.touches[0].cursorY)
}
