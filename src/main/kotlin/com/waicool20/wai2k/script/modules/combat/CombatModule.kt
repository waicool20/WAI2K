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
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.*
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import org.sikuli.basics.Settings
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import kotlin.math.min
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
        // or the map is not meant for corpse dragging
        if (mapRunner.isCorpseDraggingMap && !wasCancelled) switchDolls()
        checkRepairs()
        // Cancel further execution if any of the dolls needed to repair but were not able to
        wasCancelled = gameState.echelons.any { it.needsRepairs() }
        if (wasCancelled) return

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
        applyFilters(echelon1Doll, false)
        scanValidDolls(echelon1Doll).shuffled().first().clickRandomly()

        delay(100)
        updateEchelonRepairStatus(1)

        // Select echelon 2
        region.subRegion(120, 296, 184, 109).clickRandomly(); yield()
        // Doll 1 region ( excludes stuff below name/type )
        region.subRegion(335, 167, 263, 667).clickRandomly(); yield()

        // If sorties done is even use doll 2 else doll 1
        val echelon2Doll = ((scriptStats.sortiesDone + 1) and 1) + 1
        // Apply new filters only if they are different from the other doll
        val draggers = profile.combat.draggers
        if (draggers[1]?.stars != draggers[2]?.stars || draggers[1]?.type != draggers[2]?.type) {
            applyFilters(echelon2Doll, true)
        }
        scanValidDolls(echelon2Doll).shuffled().first().clickRandomly()

        delay(100)
        updateEchelonRepairStatus(2)
    }

    /**
     * Applies the filters ( stars and types ) in formation doll list
     */
    private suspend fun applyFilters(doll: Int, reset: Boolean) {
        logger.info("Applying doll filters for dragging doll $doll")
        val criteria = profile.combat.draggers[doll] ?: error("Invalid doll: $doll")
        val stars = criteria.stars
        val type = criteria.type
        applyDollFilters(stars, type, reset)
    }

    private var scanRetries = 0
    
    /**
     * Scans for valid dolls in the formation doll list
     *
     * @return List of regions that can be clicked to select the valid doll
     */
    private suspend fun scanValidDolls(doll: Int, retries: Int = 3): List<AndroidRegion> {
        // Wait for menu to settle starting around 300 ms
        // The amount of time waited is increased each time the doll scanning fails
        // up to a maximum of 600 ms
        delay(min(600L, 300L + scanRetries * 100))

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
                                image.getSubimage(it.x + 67, it.y + 79, 165, 40),
                                image.getSubimage(it.x + 184, it.y + 129, 39, 27),
                                region.subRegion(it.x - 7, it.y, 244, 164)
                        )
                    }
                    // Filter by name
                    .also { logger.debug("Filtering by name  ---------------------") }
                    .filterAsync(this) {
                        val ocrName = Ocr.forConfig(config).doOCRAndTrim(it.nameRegionImage)
                        val distance = ocrName.distanceTo(name, Ocr.OCR_DISTANCE_MAP)
                        logger.debug("Doll name ocr result: $ocrName | Distance: $distance | Threshold: $OCR_THRESHOLD")
                        distance < OCR_THRESHOLD
                    }
                    // Filter by level
                    .also { logger.debug("Filtering by level ---------------------") }
                    .filterAsync(this) {
                        it.levelRegionImage.binarizeImage().pad(20, 10, Color.WHITE).let { bi ->
                            val ocrLevel = Ocr.forConfig(config, digitsOnly = true).doOCRAndTrim(bi)
                            logger.debug("Level ocr result: $ocrLevel")
                            ocrLevel.toIntOrNull() ?: 1 >= level
                        }
                    }
                    // Return click regions
                    .map { it.clickRegion }
                    .also {
                        if (it.isEmpty()) {
                            logger.info("Failed to find dragging doll $doll with given criteria after ${i + 1} attempts, retries remaining: ${retries - i - 1}")
                        } else {
                            scanRetries++
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
                        image.getSubimage(it.x - 157, it.y - 188, 257, 52) to
                                image.getSubimage(it.x - 75, it.y - 97, 159, 33)
                    }.map { (nameImage, hpImage) ->
                        async {
                            Ocr.forConfig(config).doOCRAndTrim(nameImage)
                        } to async {
                            Ocr.forConfig(config).doOCRAndTrim(hpImage.binarizeImage(0.35).scale(4.0))
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
                        hp.replace(Regex("[^\\d/]"), "")
                                .split("/").let { l ->
                                    val percent = (l[0].toDouble() / l[1].toDouble()) * 100
                                    logger.info("[Repair OCR] Name: $name | HP: $hp | HP (%): $percent")
                                    percent < profile.combat.repairThreshold
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

    private suspend fun checkRepairs() {
        navigator.navigateTo(LocationId.HOME)
        if (
                gameState.echelons.any { it.needsRepairs() } ||
                region.subRegion(1337, 290, 345, 155).has("alert.png")
        ) {
            logger.info("Repairs required")
            val members = gameState.echelons.flatMap { it.members }.filter { it.needsRepair }

            navigator.navigateTo(LocationId.REPAIR)

            while (isActive) {
                val repairSlots = region.findAllOrEmpty("combat/empty-repair.png")
                repairSlots.firstOrNull()?.clickRandomly()
                        ?: run {
                            logger.info("No available repair slots, cancelling sortie")
                            return
                        }
                delay(750)

                val screenshot = region.takeScreenshot()
                val repairRegions = region.findAllOrEmpty("doll-list/lock.png")
                        .also { logger.info("Found ${it.size} dolls on screen") }
                        .filterAsync {
                            region.subRegion(it.x - 7, it.y - 268, 243, 431).has("combat/critical-dmg.png") ||
                                    Ocr.forConfig(config).doOCRAndTrim(screenshot.getSubimage(it.x + 67, it.y + 72, 161, 52)).let { oName ->
                                        members.any { it.name.distanceTo(oName) < OCR_THRESHOLD }
                                    }
                        }.map { region.subRegion(it.x - 7, it.y - 268, 243, 431) }
                        .also { logger.info("${it.size} dolls need repair") }
                if (repairRegions.isEmpty()) {
                    // Click close popup
                    region.findOrNull("close.png")?.clickRandomly()
                    break
                }

                // Select all T-Dolls
                logger.info("Selecting dolls that need repairing")
                repairRegions.take(repairSlots.size).forEach {
                    it.clickRandomly(); yield()
                    scriptStats.repairs++
                }

                // Click ok
                region.subRegion(1768, 749, 250, 159).clickRandomly(); delay(100)
                // Use quick repair
                region.subRegion(536, 702, 118, 118).clickUntilGone("combat/quick-repair.png")
                // Click ok
                region.subRegion(1381, 710, 250, 96).clickRandomly(); delay(500)
                // Click close
                region.waitSuspending("close.png", 15)?.clickRandomly(); delay(500)
                // If dolls that needed repair is equal or less than the repair slot count then
                // no more dolls need repairs and we can exit
                if (repairRegions.size <= repairSlots.size) break
            }

            logger.info("No more dolls need repairing!")
            gameState.echelons.flatMap { it.members }.forEach { it.needsRepair = false }
        }
    }

    //</editor-fold>

    //<editor-fold desc="Map Selection and Combat">

    /**
     * Clicks the given combat map chapter
     */
    private suspend fun clickCombatChapter(chapter: Int) {
        logger.info("Choosing combat chapter $chapter")
        clickChapter(chapter)
        logger.info("At combat chapter $chapter")
        navigator.checkLogistics()
    }

    /**
     * Clicks the map given the map name, it will switch the combat type depending on the suffix
     * of the given map string, 'N' for night battle and 'E for emergency battles
     * else it is assumed to be a normal map
     */
    private suspend fun clickCombatMap(map: String) {
        logger.info("Choosing combat map $map")
        navigator.checkLogistics()
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
        navigator.checkLogistics()

        // Swipe up if map is > 4
        if (map.drop(2).take(1).toInt() > 4) {
            region.subRegion(1020, 880, 675, 140).randomLocation().let {
                region.swipeRandomly(it, it.offset(0, -650))
            }
        }
        delay(200)
        // Narrow vertical region containing the map names, 1-1, 1-2 etc.
        val findRegion = region.subRegion(1060, 336, 150, 744)
        val asset = "combat/maps/${map.replace(Regex("[enEN]"), "")}.png"
        // Click until map asset is gone
        withTimeoutOrNull(10000) {
            while (isActive) {
                navigator.checkLogistics()
                val mapRegion = findRegion.findOrNull(asset)
                        ?.let { region.subRegion(it.x - 342, it.y, 1274, 50) }
                if (mapRegion != null) {
                    navigator.checkLogistics()
                    mapRegion.clickRandomly()
                    break
                }
                delay(200)
            }
        }
    }

    /**
     * Enters the map and waits for the start button to appear
     */
    private suspend fun enterBattle(map: String) {
        // Enter battle, use higher similarity threshold to exclude possibly disabled
        // button which will be slightly transparent
        var loops = 0
        while (isActive) {
            navigator.checkLogistics()
            logger.info("Entering normal battle at $map")
            // Needed in case of continue
            yield()
            // If still can't enter normal battle after 5 loops then just cancel the sortie
            // and try again
            if (loops++ == 5) return
            region.subRegion(790, 800, 580, 140)
                    .findOrNull("combat/battle/normal.png")?.clickRandomly() ?: continue
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
            if (region.waitSuspending("combat/battle/start.png", 15) != null) {
                logger.info("Entered map $map")
                break
            }
        }

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
        while (region.doesntHave(asset, 0.98)) {
            logger.info("Zoom anchor not found, attempting to zoom out")
            // Zoom in slightly randomly in case to jumble things up,
            // helps keeps thing from getting stuck
            region.pinch(
                    region.center,
                    Random.nextInt(20, 50),
                    Random.nextInt(50, 70),
                    Random.nextDouble(-10.0, 10.0),
                    300
            )
            // Then zoom out
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
        region.mouseDelay(0.0) { mapRunner.execute() }
        // Restore script threshold
        Settings.MinSimilarity = config.scriptConfig.defaultSimilarityThreshold
    }
    //</editor-fold>
}