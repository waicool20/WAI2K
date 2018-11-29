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
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.util.CombatChapter
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.*
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.sikuli.basics.Settings
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import kotlin.math.abs
import kotlin.random.Random
import kotlin.reflect.full.primaryConstructor

private const val OCR_THRESHOLD = 2

class CombatModule(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {
    private val logger = loggerFor<CombatModule>()
    private val mapRunner = MapRunner.list[profile.combat.map]?.primaryConstructor
            ?.call(scriptRunner, region, config, profile) ?: error("Unsupported map")

    private var wasCancelled = false

    override suspend fun execute() {
        if (!profile.combat.enabled) return
        // Return if the base doll limit is already reached
        if (gameState.dollOverflow) return
        runCombatCycle()
    }

    /**
     * Runs a combat cycle
     */
    private suspend fun runCombatCycle() {
        // Don't need to switch dolls if previous run was cancelled
        if (!wasCancelled) switchDolls()
        val map = profile.combat.map
        navigator.navigateTo(LocationId.COMBAT)
        clickCombatChapter(map.take(1).toInt())
        clickCombatMap(map)
        enterBattle(map)
        // Cancel further execution if not in battle, maybe due to doll/equip overflow
        wasCancelled = gameState.currentGameLocation.id != LocationId.BATTLE
        if (wasCancelled) return

        zoomMap(map)
        executeMapRunner()

        // Set game location back to combat menu now that battle has ended
        gameState.currentGameLocation = GameLocation.mappings(config)[LocationId.COMBAT_MENU] ?: error("Bad locations.json file")
        logger.info("Sortie complete")
        // Back to combat menu or home, check logistics
        navigator.checkLogistics()
    }

    //<editor-fold desc="Doll Switching">

    /**
     * Switches the dolls in the echelons who will be dragging, which doll goes into which
     * echelon depends on the sortie cycle. On even sortie cycles (ie. 0, 2, 4...)
     * doll 1 goes into echelon 1, doll 2 goes into echelon 2 and vice versa
     */
    private suspend fun switchDolls() {
        navigator.navigateTo(LocationId.FORMATION)
        logger.info("Switching doll 2 of echelon 1")
        // Doll 2 region ( excludes stuff below name/type )
        region.subRegion(612, 167, 263, 667).clickRandomly(); yield()

        // If sorties done is even use doll 1 else doll 2
        val echelon1Doll = (scriptStats.sortiesDone and 1) + 1
        applyFilters(echelon1Doll)
        scanValidDolls(echelon1Doll).shuffled().first().clickRandomly()

        delay(100)
        updateEchelonRepairStatus(1)

        // Select echelon 2
        region.subRegion(120, 296, 184, 109).clickRandomly(); yield()
        // Doll 1 region ( excludes stuff below name/type )
        region.subRegion(335, 167, 263, 667).clickRandomly(); yield()

        // If sorties done is even use doll 2 else doll 1
        val echelon2Doll = ((scriptStats.sortiesDone + 1) and 1) + 1
        applyFilters(echelon2Doll)
        scanValidDolls(echelon2Doll).shuffled().first().clickRandomly()

        delay(100)
        updateEchelonRepairStatus(2)
    }

    /**
     * Applies the filters ( stars and types ) in formation doll list
     */
    private suspend fun applyFilters(doll: Int) {
        logger.info("Applying doll filters for dragging doll $doll")
        val criteria = profile.combat.draggers[doll] ?: error("Invalid doll: $doll")
        val stars = criteria.stars
        val type = criteria.type

        delay(100)
        // Filter By button
        val filterButtonRegion = region.subRegion(1765, 348, 257, 161)
        filterButtonRegion.clickRandomly(); delay(100)
        // Filter popup region
        val prefix = "doll-list/filters"
        region.subRegion(900, 159, 834, 910).run {
            logger.info("Resetting filters")
            find("$prefix/reset.png").clickRandomly(); yield()
            filterButtonRegion.clickRandomly(); delay(100)
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

        logger.info("Attempting to find dragging doll $doll with given criteria name = $name, level >= $level")
        repeat(retries) { i ->
            // Take a screenshot after each retry, just in case it was a bad one in case its not OCRs fault
            // Optimize by taking a single screenshot and working on that
            val image = region.takeScreenshot()
            region.findAllOrEmpty("doll-list/lock.png")
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
                            abs(Ocr.forConfig(config, digitsOnly = true).doOCRAndTrim(bi).toIntOrNull()
                                    ?: 1) >= level
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
        logger.warn("Failed to find dragging doll $doll that matches criteria after $retries attempts")
        coroutineContext.cancelAndYield()
    }

    //</editor-fold>

    //<editor-fold desc="Repair">

    private suspend fun updateEchelonRepairStatus(echelon: Int, retries: Int = 3) {
        logger.info("Updating repair status")

        for (i in 1..retries) {
            val image = region.takeScreenshot()
            val members = region.findAllOrEmpty("formation/stats.png")
                    .also { logger.info("Found ${it.size} dolls on screen") }
                    .sortedBy { it.x }
                    .map {
                        // Name to HP pair
                        image.getSubimage(it.x - 118, it.y - 181, 183, 48) to
                                image.getSubimage(it.x - 75, it.y - 97, 159, 33)
                    }.map { (nameImage, hpImage) ->
                        async {
                            Ocr.forConfig(config).doOCRAndTrim(nameImage)
                        } to async {
                            Ocr.forConfig(config).doOCRAndTrim(hpImage)
                        }
                    }.map { (dName, dHp) ->
                        dName.await() to dHp.await()
                    }
            // Checking if the ocr results were gibberish
            if (members.none { it.first.distanceTo(profile.combat.draggers[1]!!.name) < OCR_THRESHOLD } &&
                    members.none { it.first.distanceTo(profile.combat.draggers[2]!!.name) < OCR_THRESHOLD }) {
                logger.info("Update repair status ocr failed after $i attempts, retries remaining: ${retries - i}")
                if (i == retries) {
                    logger.warn("Could not update repair status after $retries attempts")
                    coroutineContext.cancelAndYield()
                }
                continue
            }

            members.forEachIndexed { member, (name, hp) ->
                gameState.echelons[echelon].members[member].also {
                    it.name = name
                    it.needsRepair = try {
                        hp.split(Regex("\\D")).let { l ->
                            l[0].toDouble() / l[1].toDouble() < 0.3
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            break
        }
        logger.info("Updating repair status complete")
        gameState.echelons[echelon].members.forEachIndexed { i, member ->
            logger.info("[Echelon $echelon member ${i + 1}] Name: ${member.name} | Needs repairs: ${member.needsRepair}")
        }
    }

    //</editor-fold>

    //<editor-fold desc="Map Selection and Combat">

    /**
     * Clicks the given combat map chapter
     */
    private suspend fun clickCombatChapter(chapter: Int) {
        logger.info("Choosing combat chapter $chapter")
        CombatChapter.clickChapter(chapter, region)
        logger.info("At combat chapter $chapter")
    }

    /**
     * Clicks the map given the map name, it will switch the combat type depending on the suffix
     * of the given map string, 'N' for night battle and 'E for emergency battles
     * else it is assumed to be a normal map
     */
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
        val findRegion = region.subRegion(1060, 336, 150, 744)
        // Click until map asset is gone
        withTimeoutOrNull(10000) {
            val asset = "combat/maps/${map.replace(Regex("[enEN]"), "")}.png"
            while (findRegion.has(asset)) {
                yield()
                findRegion.findOrNull(asset)
                        // Map to whole length of map entry, can't do height as it might not be
                        // on screen
                        ?.let { region.subRegion(it.x - 342, it.y, 1274, 50) }
                        ?.clickRandomly()
            }
        }
    }

    /**
     * Enters the map and waits for the start button to appear
     */
    private suspend fun enterBattle(map: String) {
        // Enter battle, use higher similarity threshold to exclude possibly disabled
        // button which will be slightly transparent
        logger.info("Entering normal battle at $map")
        region.subRegion(790, 800, 580, 140)
                .clickUntilGone("combat/battle/normal.png", 10, 0.96)
        delay(200)

        region.subRegion(1185, 696, 278, 95).findOrNull("combat/enhancement.png")?.apply {
            logger.info("T-doll limit reached, cancelling sortie")
            clickRandomly()
            gameState.dollOverflow = true
            gameState.currentGameLocation = GameLocation.mappings(config)[LocationId.TDOLL_ENHANCEMENT] ?: error("Bad locations.json file")
            return
        }

        // Wait for start operation button to appear first before handing off control to
        // map specific files
        region.waitSuspending("combat/battle/start.png", 15)
        logger.info("Entered map $map")

        // Set location to battle
        gameState.currentGameLocation = GameLocation(LocationId.BATTLE)
    }

    /**
     * Checks if a given map has a given asset called 'zoom-anchor.png' and tries to zoom out until
     * it can find that asset on the screen
     */
    private suspend fun zoomMap(map: String) {
        val asset = "combat/maps/${map.toUpperCase()}/zoom-anchor.png"
        if (Files.notExists(config.assetsDirectory.resolve(asset))) return
        while (region.doesntHave(asset)) {
            logger.info("Zoom anchor not found, attempting to zoom out")
            region.pinch(
                    region.center,
                    Random.nextInt(500, 700),
                    Random.nextInt(20, 50),
                    Random.nextDouble(-10.0, 10.0),
                    2500
            )
            delay(1000)
        }
        logger.info("Zoom anchor found, ready to begin map")
    }

    /**
     * Sets the similarity threshold to map runner settings then executes the map runner,
     * it restores the default similarity threshold before finishing
     */
    private suspend fun executeMapRunner() {
        // Set similarity to slightly lower threshold for discrepancies because of zoom level
        Settings.MinSimilarity = config.scriptConfig.mapRunnerSimilarityThreshold
        mapRunner.execute()
        // Restore script threshold
        Settings.MinSimilarity = config.scriptConfig.defaultSimilarityThreshold
    }
    //</editor-fold>
}