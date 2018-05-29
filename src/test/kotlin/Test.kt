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

import com.waicool20.wai2k.android.AndroidDevice
import org.sikuli.script.ImagePath
import org.sikuli.script.Location
import java.util.*

fun main(args: Array<String>) {
    ImagePath.add(ClassLoader.getSystemClassLoader().getResource("images"))
    val device = AndroidDevice.listAll().first()
    device.displayPointerInfo(true)
    val random = Random()
    while (true) {
        device.screen.dragDrop(Location(random.nextInt(900) + 200, random.nextInt(900) + 200), Location(random.nextInt(900) + 200, random.nextInt(900) + 200))
    }
}
