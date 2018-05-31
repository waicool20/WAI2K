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

package com.waicool20.util

import com.waicool20.util.logging.loggerFor
import org.sikuli.script.ImagePath
import org.sikuli.script.Screen
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

object SikuliXLoader {
    private val logger = loggerFor<SikuliXLoader>()

    private var _working = false
    val SIKULI_WORKING get() = _working

    fun loadAndTest(path: Path) {
        logger.info("Loading Sikulix Jar at ${path.toAbsolutePath()}")
        SystemUtils.loadJarLibrary(path)
        logger.info("SikuliX jar now loaded in classpath")
        testSikuliX()
    }

    fun loadImagePath(path: Path) {
        ImagePath.add(path.toUri().toURL())
    }

    private fun testSikuliX() {
        try {
            preventSystemExit {
                logger.info("Testing SikuliX")
                // Delay is needed to prevent hanging
                TimeUnit.MILLISECONDS.sleep(100)
                logger.info("Testing screen: ${Screen()}")
                logger.info("Test image loading")
                Files.createTempDirectory("sikulix-test-temp").also { temp ->
                    ImagePath.add(temp.toUri().toURL())
                    ImagePath.remove("$temp")
                    Files.deleteIfExists(temp)
                }
                logger.info("Image loading passed")
                _working = true
            }
        } catch (e: NoClassDefFoundError) {
            logger.warn("SikuliX classes not found")
        } catch (e: IllegalExitException) {
            logger.warn("SikuliX ran into a fatal error and tried to exit the program")
            logger.warn("SikuliX installation might be broken! Go reinstall!")
        }
    }
}
