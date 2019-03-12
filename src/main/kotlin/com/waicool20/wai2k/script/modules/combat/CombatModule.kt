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
import com.waicool20.wai2k.config.Wai2KProfile.DollCriteria
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.wai2k.util.useLegacyEngine
import com.waicool20.waicoolutils.*
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import org.sikuli.basics.Settings
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.text.DecimalFormat
import kotlin.math.min
import kotlin.math.roundToLong
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

    companion object {
        private val dollSwitchingCache = mutableMapOf<DollCriteria, AndroidRegion>()
    }

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
        if (mapRunner.isCorpseDraggingMap && !wasCancelled) {
            switchDolls()
            // Check if there was a bad switch
            if (wasCancelled) {
                logger.info("Bad switch, maybe the doll positions got shifted, cancelling this run")
                dollSwitchingCache.clear()
                return
            }
        }
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
        // Cancel further execution if not in battle, maybe due to bad zoom
        wasCancelled = gameState.currentGameLocation.id != LocationId.BATTLE
        if (wasCancelled) return

        executeMapRunner()

        // Set game location back to combat menu now that battle has ended
        gameState.currentGameLocation = GameLocation.mappings(config)[LocationId.COMBAT_MENU] ?: error("Bad locations.json file")
        logger.info("Sortie complete")

        //if terminated back to home, check for daily reset
        navigator.checkDailyReset()
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
        val startTime = System.currentTimeMillis()
        logger.info("Switching doll 2 of echelon 1")
        // Doll 2 region ( excludes stuff below name/type )
        region.subRegion(612, 167, 263, 667).clickRandomly(); yield()
        region.waitSuspending("doll-list/lock.png", 5)

        val draggers = profile.combat.draggers
        // If sorties done is even use doll 1 else doll 2
        val echelon1Doll = (scriptStats.sortiesDone and 1) + 1
        // If sorties done is odd use doll 2 else doll 1
        val echelon2Doll = ((scriptStats.sortiesDone + 1) and 1) + 1
        val dragger1 = draggers[echelon1Doll]!!
        val dragger2 = draggers[echelon2Doll]!!
        val sameDragger = dragger1.name == dragger2.name
        var scanResults = emptyList<AndroidRegion>()

        applyFilters(echelon1Doll, false)
        dollSwitchingCache.getOrPut(dragger1) {
            scanResults = scanValidDolls(echelon1Doll)
            scanResults[dragger1.index]
        }.clickRandomly()
        delay(400)
        updateEchelonRepairStatus(1)

        // Select echelon 2
        region.subRegion(120, 296, 184, 109).clickRandomly(); delay(200)
        // Doll 1 region ( excludes stuff below name/type )
        region.subRegion(335, 167, 263, 667).clickRandomly(); yield()
        region.waitSuspending("doll-list/lock.png", 5)

        // Apply new filters only if they are different from the other doll
        if (dragger1.stars != dragger2.stars || dragger1.type != dragger2.type) {
            applyFilters(echelon2Doll, true)
        }
        dollSwitchingCache.getOrPut(dragger2) {
            if (!sameDragger || scanResults.isEmpty()) scanResults = scanValidDolls(echelon2Doll)
            scanResults[dragger2.index]
        }.clickRandomly()
        delay(400)
        updateEchelonRepairStatus(2)

        // Check if dolls were switched correctly, might not be the case if one of them leveled
        // up and the positions got switched
        wasCancelled = gameState.echelons[0].members[1].name
                .distanceTo(profile.combat.draggers[echelon1Doll]!!.name) >= OCR_THRESHOLD ||
                gameState.echelons[1].members[0].name
                        .distanceTo(profile.combat.draggers[echelon2Doll]!!.name) >= OCR_THRESHOLD
        logger.info("Switching dolls took ${System.currentTimeMillis() - startTime} ms")
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
        delay(500)
        region.waitSuspending("doll-list/lock.png", 5)
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
        // up to a maximum of 1000 ms
        delay(min(1000L, 300L + scanRetries * 100))

        logger.info("Scanning for valid dolls in filtered list for dragging doll $doll")
        val criteria = profile.combat.draggers[doll] ?: error("Invalid doll: $doll")
        val name = criteria.name
        val level = criteria.level

        // Temporary convenience class for storing doll regions
        class DollRegions(nameRegionImage: BufferedImage, levelRegionImage: BufferedImage, val clickRegion: AndroidRegion) {
            val name = async {
                Ocr.forConfig(config).doOCRAndTrim(nameRegionImage)
            }
            val level = async {
                val i = levelRegionImage.pad(30, 30, Color.WHITE).binarizeImage().scale()
                val ocr = Ocr.forConfig(config, digitsOnly = true, useLSTM = true)
                ocr.doOCRAndTrim(i).toIntOrNull()
                        ?: ocr.useLegacyEngine().doOCRAndTrim(i).toIntOrNull()
            }
        }

        logger.info("Attempting to find dragging doll $doll with given criteria name = $name, distance < $OCR_THRESHOLD, level >= $level")
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
                    }.filter {
                        val ocrName = it.name.await()
                        val ocrLevel = it.level.await()
                        val distance = ocrName.distanceTo(name, Ocr.OCR_DISTANCE_MAP)
                        logger.debug("[Scan OCR] Name: $ocrName | Distance: $distance | Level: $ocrLevel")
                        distance < OCR_THRESHOLD && ocrLevel ?: 1 >= level
                    }
                    // Return click regions
                    .map { it.clickRegion }
                    .also {
                        if (it.isEmpty()) {
                            logger.info("Failed to find dragging doll $doll with given criteria after ${i + 1} attempts, retries remaining: ${retries - i - 1}")
                        } else {
                            scanRetries++
                            logger.info("Found ${it.size} dolls that match the criteria for doll $doll")
                            return it.sortedBy { it.y * 10 + it.x }
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

        // Temporary convenience class for storing doll regions
        class DollRegions(nameImage: BufferedImage, hpImage: BufferedImage) {
            private val _name = async {
                Ocr.forConfig(config).doOCRAndTrim(nameImage)
            }
            private val _percent = async {
                val image = hpImage.binarizeImage()
                var whites = 0.0
                repeat(image.width) { i -> if (image.getRGB(i, 0) == Color.WHITE.rgb) whites++ }
                whites / image.width * 100
            }
            val name by lazy { runBlocking { _name.await() } }
            val percent by lazy { runBlocking { _percent.await() } }
        }

        for (i in 1..retries) {
            val image = region.takeScreenshot()
            val members = region.findAllOrEmpty("formation/stats.png")
                    .also { logger.info("Found ${it.size} dolls on screen") }
                    .sortedBy { it.x }
                    .map {
                        DollRegions(
                                image.getSubimage(it.x - 157, it.y - 188, 257, 52),
                                image.getSubimage(it.x - 139, it.y - 55, 221, 1)
                        )
                    }
            // Checking if the ocr results were gibberish
            if (members.none { it.name.distanceTo(profile.combat.draggers[1]!!.name) < OCR_THRESHOLD } &&
                    members.none { it.name.distanceTo(profile.combat.draggers[2]!!.name) < OCR_THRESHOLD }) {
                logger.info("Update repair status ocr failed after $i attempts, retries remaining: ${retries - i}")
                if (i == retries) {
                    logger.warn("Could not update repair status after $retries attempts, continue anyways")
                    break
                }
                continue
            }

            val formatter = DecimalFormat("##.#")
            gameState.echelons[echelon - 1].members.forEachIndexed { j, member ->
                val dMember = members.getOrNull(j)
                member.name = dMember?.name ?: "Unknown"
                member.needsRepair = (dMember?.percent ?: 100.0) < profile.combat.repairThreshold
                val sPercent = dMember?.percent?.let { formatter.format(it) } ?: "N/A"
                logger.info("[Repair OCR] Name: ${member.name} | HP (%): $sPercent")
            }
            break
        }
        logger.info("Updating repair status complete")
        gameState.echelons[echelon - 1].members.forEachIndexed { i, member ->
            logger.info("[Echelon $echelon member ${i + 1}] Name: ${member.name} | Needs repairs: ${member.needsRepair}")
        }
    }

    private suspend fun checkRepairs() {
        logger.info("Checking for repairs")
        suspend fun checkHome(): Boolean {
            return if ((scriptStats.sortiesDone - 1) % profile.combat.repairCheckFrequency == 0) {
                logger.info("Checking home for repairs, next check at ${scriptStats.sortiesDone + profile.combat.repairCheckFrequency} sorties")
                navigator.navigateTo(LocationId.HOME)
                region.waitSuspending("locations/repair.png", 10)
                region.subRegion(1337, 290, 345, 155).has("alert.png")
            } else false
        }

        if (gameState.echelons.any { it.needsRepairs() } || checkHome()) {
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
                delay(1000)

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
                region.mouseDelay(0.0) {
                    repairRegions.take(repairSlots.size).sortedBy { it.y * 10 + it.x }.forEach {
                        it.clickRandomly(); yield()
                        scriptStats.repairs++
                    }
                }

                // Click ok
                region.subRegion(1768, 749, 250, 159).clickRandomly(); delay(100)
                // Use quick repair
                region.subRegion(545, 713, 99, 96).clickRandomly(); yield()
                // Click ok
                region.subRegion(1381, 710, 250, 96).clickRandomly(); yield()
                // Click close
                region.waitSuspending("close.png", 15)?.clickRandomly()
                // If dolls that needed repair is equal or less than the repair slot count then
                // no more dolls need repairs and we can exit
                if (repairRegions.size <= repairSlots.size) break
                delay(500)
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
        var zooms = 0
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
            if (zooms++ >= 5) {
                // Click select operation to go back to combat menu
                region.subRegion(11, 14, 191, 110).clickRandomly()
                gameState.currentGameLocation = GameLocation(LocationId.COMBAT)
                return
            }
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