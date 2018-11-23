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

package com.waicool20.wai2k.script.modules.combat

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.util.CombatChapter
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.*
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.awt.Color
import java.awt.image.BufferedImage

private const val OCR_THRESHOLD = 2

class CombatModule(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {
    private val logger = loggerFor<CombatModule>()

    override suspend fun execute() {
        if (!profile.combat.enabled) return
        //switchDolls()
        navigator.navigateTo(LocationId.COMBAT)
        clickCombatChapter(profile.combat.map.take(1).toInt())
        clickCombatMap(profile.combat.map)
    }

    //<editor-fold desc="Doll Switching">

    private suspend fun switchDolls() {
        navigator.navigateTo(LocationId.FORMATION)
        logger.info("Switching doll 2 of echelon 1")
        // Doll 2 region ( excludes stuff below name/type )
        region.subRegion(612, 167, 263, 667).clickRandomly(); yield()
        applyFilters(1)
        scanValidDolls(1).shuffled().first().clickRandomly()

        // Select echelon 2
        region.subRegion(120, 296, 184, 109).clickRandomly(); yield()
        // Doll 1 region ( excludes stuff below name/type )
        region.subRegion(335, 167, 263, 667).clickRandomly(); yield()
        applyFilters(2)
        scanValidDolls(2).shuffled().first().clickRandomly()
    }

    /**
     * Applies the filters ( stars and types ) in formation doll list
     */
    private suspend fun applyFilters(doll: Int) {
        logger.info("Applying doll filters for dragging doll $doll")
        val criteria = profile.combat.draggers[doll] ?: error("Invalid doll: $doll")
        val stars = criteria.stars
        val type = criteria.type

        // Filter By button
        val filterButtonRegion = region.subRegion(1765, 348, 257, 161)
        filterButtonRegion.clickRandomly(); yield()
        // Filter popup region
        val prefix = "combat/formation/filters"
        region.subRegion(900, 159, 834, 910).run {
            logger.info("Resetting filters")
            find("$prefix/reset.png").clickRandomly(); yield()
            filterButtonRegion.clickRandomly(); yield()
            logger.info("Applying filter $stars star")
            find("$prefix/${stars}star.png").clickRandomly(); yield()
            logger.info("Applying filter $type")
            find("$prefix/$type.png").clickRandomly(); yield()
            logger.info("Confirming filters")
            find("$prefix/confirm.png").clickRandomly(); yield()
        }
        // Wait for menu to settle
        delay(150)
    }

    /**
     * Scans for valid dolls in the formation doll list
     *
     * @return List of regions that can be clicked to select the valid doll
     */
    private suspend fun scanValidDolls(doll: Int, retries: Int = 3): List<AndroidRegion> {
        logger.info("Scanning for valid dolls in filtered list for dragging doll $doll")
        val criteria = profile.combat.draggers[doll] ?: error("Invalid doll: $doll")
        val name = criteria.name
        val level = criteria.level

        // Temporary convenience class for storing doll regions
        class DollRegions(val nameRegionImage: BufferedImage, val levelRegionImage: BufferedImage, val clickRegion: AndroidRegion)

        logger.info("Attempting to find dragging doll $doll with given criteria name = $name, level > $level")
        repeat(retries) { i ->
            // Take a screenshot after each retry, just in case it was a bad one in case its not OCRs fault
            // Optimize by taking a single screenshot and working on that
            val image = region.takeScreenshot()
            region.findAllOrEmpty("combat/formation/lock.png")
                    .also { logger.info("Found ${it.size} dolls on screen") }
                    // Transform the lock region into 3 distinct regions used for ocr and clicking by offset
                    .map {
                        DollRegions(
                                image.getSubimage(it.x + 67, it.y + 72, 161, 52),
                                image.getSubimage(it.x + 183, it.y + 124, 45, 32),
                                region.subRegion(it.x - 7, it.y, 244, 164)
                        )
                    }
                    // Filter by name
                    .filterAsync(this) {
                        val ocrName = Ocr.forConfig(config).doOCRAndTrim(it.nameRegionImage)
                        val distance = ocrName.distanceTo(name, Ocr.OCR_DISTANCE_MAP)
                        logger.debug("Doll name ocr result: $ocrName | Distance: $distance | Threshold: $OCR_THRESHOLD")
                        distance < OCR_THRESHOLD
                    }
                    // Filter by level
                    .filterAsync(this) {
                        it.levelRegionImage.binarizeImage().scale(2.0).pad(20, 10, Color.WHITE).let { bi ->
                            Ocr.forConfig(config, digitsOnly = true).doOCRAndTrim(bi).toIntOrNull() ?: 0 > level
                        }
                    }
                    // Return click regions
                    .map { it.clickRegion }
                    .also {
                        if (it.isEmpty()) {
                            logger.info("Failed to find dragging doll $doll with given criteria after ${i + 1} attempts, retries remaining: ${retries - i - 1}")
                        } else {
                            logger.info("Found ${it.size} dolls that match the criteria for doll $doll")
                            return it
                        }
                    }
        }
        error("Failed to find dragging doll $doll that matches criteria after $retries attempts")
    }

    //</editor-fold>

    //<editor-fold desc="Map Selection">

    private suspend fun clickCombatChapter(chapter: Int) {
        logger.info("Choosing combat chapter $chapter")
        CombatChapter.clickChapter(chapter, region)
        logger.info("At combat chapter $chapter")
    }

    private suspend fun clickCombatMap(map: String) {
        logger.info("Choosing combat map $map")
        when {
            map.endsWith("e", true) -> {
                logger.info("Selecting emergency map")
                region.subRegion(1709, 265, 125, 25).clickRandomly()
            }
            map.endsWith("n", true) -> {
                logger.info("Selecting night map")
                region.subRegion(1871, 265, 142, 25).clickRandomly()
            }
            else -> {
                // Just in case, shouldn't be needed
                // region.subRegion(1558, 265, 84, 25).clickRandomly()
            }
        }
        yield()

        // TODO: Implement scrolling for other maps
        // Narrow vertical region containing the map names, 1-1, 1-2 etc.
        region.subRegion(1089, 336, 80, 744)
                .clickUntilGone("combat/maps/${map.replace(Regex("[enEN]"), "")}.png", 10)
    }

    //</editor-fold>
}