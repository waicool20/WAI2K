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

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.asCachedRegion
import com.waicool20.cvauto.core.input.ITouchInterface
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.core.template.ImageTemplate
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.game.TDoll
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.script.modules.combat.maps.EventMapRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.binarizeImage
import com.waicool20.waicoolutils.countColor
import com.waicool20.waicoolutils.filterAsync
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.text.DecimalFormat
import kotlin.reflect.full.primaryConstructor

class CombatModule(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {
    private val logger = loggerFor<CombatModule>()
    private val mapRunner by lazy {
        MapRunner.list[profile.combat.map]?.primaryConstructor
                ?.call(scriptRunner, region, config, profile) ?: error("Unsupported map")
    }

    private var wasCancelled = false

    override suspend fun execute() {
        if (scriptRunner.justRestarted) gameState.requiresMapInit = true
        if (!profile.combat.enabled) return
        // Return if the base doll limit is already reached
        if (gameState.dollOverflow) return
        // Return if the equip limit is already reached
        if (gameState.equipOverflow) return
        // Return if echelon 1 has repairs
        if (gameState.echelons[0].hasRepairs()) return
        // Also Return if its a corpse dragging map and echelon 2 has repairs
        if (mapRunner.isCorpseDraggingMap && gameState.echelons[1].hasRepairs()) return
        if (MapRunner.eventMaps.contains(profile.combat.map)) {
            runEventCombatCycle()
        } else {
            runCombatCycle()
        }
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

        executeMapRunner()

        // Set game location back to combat menu now that battle has ended
        gameState.currentGameLocation = GameLocation.mappings(config)[LocationId.COMBAT_MENU]
                ?: error("Bad locations.json file")
        logger.info("Sortie complete")
        // Back to combat menu or home, check logistics
        navigator.checkLogistics()
    }

    private suspend fun runEventCombatCycle() {
        checkRepairs()
        // Cancel further execution if any of the dolls needed to repair but were not able to
        wasCancelled = gameState.echelons.any { it.needsRepairs() }
        if (wasCancelled) return
        navigator.navigateTo(LocationId.EVENT)

        (mapRunner as EventMapRunner).enterMap()
        if (checkNeedsEnhancement()) return
        executeMapRunner()

        gameState.currentGameLocation = GameLocation.mappings(config)[LocationId.EVENT]
                ?: error("Bad locations.json file")
        logger.info("Sortie complete")
    }

    //<editor-fold desc="Doll Switching">

    /**
     * Switches the dolls in the echelons who will be dragging, which doll goes into which
     * echelon depends on the sortie cycle. On even sortie cycles (ie. 0, 2, 4...)
     * doll 1 goes into echelon 1, doll 2 goes into echelon 2 and vice versa
     */
    private suspend fun switchDolls() {
        navigator.navigateTo(LocationId.FORMATION)
        delay(1000) // Formation takes a while to load/render

        if (scriptStats.sortiesDone >= 1) {
            val startTime = System.currentTimeMillis()
            logger.info("Switching doll 2 of echelon 1")
            // Doll 2 region ( excludes stuff below name/type )
            region.subRegion(635, 206, 237, 544).click(); yield()
            region.waitHas(FileTemplate("doll-list/lock.png"), 5000)

            var switchDoll: Region<AndroidDevice>? = null
            region.matcher.settings.matchDimension = ScriptRunner.HIGH_RES
            val tdolls = profile.combat.draggers
                    .map { TDoll.lookup(config, it.id) ?: error("Invalid doll: ${it.id}") }
                    // Distinct filter types that way we dont set filters twice for same filter
                    .distinctBy { it.type.ordinal * 10 + it.stars }
            for ((i, tdoll) in tdolls.withIndex()) {
                applyFilters(tdoll, i == 1)
                switchDoll = region.findBest(FileTemplate("doll-list/echelon2-captain.png"))?.region

                val r1 = region.subRegionAs<AndroidRegion>(1210, 1038, 500, 20)
                val r2 = r1.copyAs<AndroidRegion>(y = r1.y - 750)
                val checkRegion = region.subRegion(185, 360, 60, 60)

                var scrollDown = true
                var checkImg: BufferedImage

                withTimeoutOrNull(90_000) {
                    val r = region.subRegion(167, 146, 1542, 934)
                    while (isActive) {
                        // Trying this to improve search reliability, maybe put this upstream in cvauto
                        switchDoll = r.findBest(FileTemplate("doll-list/echelon2-captain.png", 0.85), 5)
                                .maxBy { it.score }?.region
                        if (switchDoll == null) {
                            checkImg = checkRegion.capture()
                            if (scrollDown) {
                                r1.swipeTo(r2, 500)
                            } else {
                                r2.swipeTo(r1, 500)
                            }
                            delay(2000)
                            if (checkRegion.has(ImageTemplate(checkImg))) {
                                logger.info("Reached ${if (scrollDown) "bottom" else "top"} of the list")
                                scrollDown = !scrollDown
                            }
                        } else break
                    }
                } ?: error("Timed out finding a replacement dragging doll")

                if (switchDoll != null) {
                    switchDoll?.copy(width = 142)?.click()
                    break
                }
            }
            if (switchDoll == null) error("Could not find replacement dragging doll")
            region.matcher.settings.matchDimension = ScriptRunner.NORMAL_RES
            logger.info("Switching dolls took ${System.currentTimeMillis() - startTime} ms")
            delay(400)
        }

        updateEchelonRepairStatus(1)

        // Check if dolls were switched correctly, might not be the case if one of them leveled
        // up and the positions got switched
        val echelon1Members = gameState.echelons[0].members.map { it.name }
        wasCancelled = profile.combat.draggers.none { TDoll.lookup(config, it.id)?.name in echelon1Members }
    }

    /**
     * Applies the filters ( stars and types ) in formation doll list
     */
    private suspend fun applyFilters(tdoll: TDoll, reset: Boolean) {
        logger.info("Applying doll filters for dragging doll ${tdoll.name}")
        tdoll.apply { applyDollFilters(stars, type, reset) }
        delay(500)
        region.subRegion(1188, 214, 258, 252)
                .waitDoesntHave(FileTemplate("doll-list/filtermenu.png"), 5000)
    }

    //</editor-fold>

    //<editor-fold desc="Repair">

    private suspend fun updateEchelonRepairStatus(echelon: Int, retries: Int = 3) {
        logger.info("Updating repair status")

        // Temporary convenience class for storing doll regions
        class DollRegions(nameImage: BufferedImage, hpImage: BufferedImage) {
            val tdollOcr = async {
                val ocr = Ocr.forConfig(config).doOCRAndTrim(nameImage.binarizeImage(0.72))
                val tdoll = TDoll.lookup(config, ocr)
                ocr to tdoll
            }
            val percent = async {
                val image = hpImage.binarizeImage()
                image.countColor(Color.WHITE) / image.width.toDouble() * 100
            }
        }

        for (i in 1..retries) {
            val cache = region.asCachedRegion()
            val members = cache.findBest(FileTemplate("formation/stats.png"), 5)
                    .also { logger.info("Found ${it.size} dolls on screen") }
                    .map { it.region }
                    .sortedBy { it.x }
                    .map {
                        DollRegions(
                                cache.capture().getSubimage(it.x - 153, it.y - 183, 247, 84),
                                cache.capture().getSubimage(it.x - 133, it.y - 61, 209, 1)
                        )
                    }

            val formatter = DecimalFormat("##.#")
            gameState.echelons[echelon - 1].members.forEachIndexed { j, member ->
                val dMember = members.getOrNull(j)
                member.name = dMember?.tdollOcr?.await()?.second?.name ?: "Unknown"
                member.needsRepair = (dMember?.percent?.await()
                        ?: 100.0) < profile.combat.repairThreshold
                val sPercent = dMember?.percent?.await()?.let { formatter.format(it) } ?: "N/A"
                logger.info("[Repair OCR] Name: ${dMember?.tdollOcr?.await()?.first} | HP (%): $sPercent")
            }

            // Checking if the ocr results were gibberish
            // Skip check if game state hasnt been initialized yet
            val member2 = members.getOrNull(1)?.tdollOcr?.await()?.second?.name
            if (member2 == null || profile.combat.draggers.none { it.id.contains(member2) }) {
                logger.info("Update repair status ocr failed after $i attempts, retries remaining: ${retries - i}")
                if (i == retries) {
                    logger.warn("Could not update repair status after $retries attempts")
                    logger.warn("Check if you set the right T doll as dragger")
                    coroutineContext.cancelAndYield()
                }
                continue
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
        if (gameState.echelons.any { it.needsRepairs() }) {
            logger.info("Repairs required")
            val members = gameState.echelons.flatMap { it.members }.filter { it.needsRepair }

            navigator.navigateTo(LocationId.REPAIR)

            while (isActive) {
                val repairSlots = region.findBest(FileTemplate("combat/empty-repair.png"), 7)
                        .map { it.region }
                repairSlots.firstOrNull()?.click()
                        ?: run {
                            logger.info("No available repair slots, cancelling sortie")
                            return
                        }
                delay(1000)

                val cache = region.asCachedRegion()
                // Set matcher to high resolution, otherwise sometimes not all lock.png are found
                region.matcher.settings.matchDimension = ScriptRunner.HIGH_RES
                val repairRegions = cache.findBest(FileTemplate("doll-list/lock.png"), 12)
                        .also { logger.info("Found ${it.size} dolls on screen") }
                        .map { it.region }
                        .filterAsync {
                            val isCritical = region.subRegion(it.x - 4, it.y - 258, 230, 413).has(FileTemplate("combat/critical-dmg.png"))
                            val isDmgedMember = Ocr.forConfig(config).doOCRAndTrim(cache.capture().getSubimage(it.x + 61, it.y + 77, 166, 46))
                                    .let { TDoll.lookup(config, it) }
                                    ?.let { tdoll -> members.any { it.name == tdoll.name } } == true
                            isCritical || isDmgedMember
                        }.map { region.subRegion(it.x - 4, it.y - 258, 230, 413) }
                        .also { logger.info("${it.size} dolls need repair") }
                region.matcher.settings.matchDimension = ScriptRunner.NORMAL_RES
                if (repairRegions.isEmpty()) {
                    // Click close popup
                    region.findBest(FileTemplate("close.png"))?.region?.click()
                    break
                }

                // Select all T-Dolls
                logger.info("Selecting dolls that need repairing")
                repairRegions.take(repairSlots.size).sortedBy { it.y * 10 + it.x }.forEach {
                    it.click(); yield()
                    scriptStats.repairs++
                }

                // Click ok
                region.subRegion(1888, 749, 250, 158).click(); delay(100)
                // Use quick repair
                region.subRegion(545, 713, 99, 96).click(); yield()
                // Click ok
                region.subRegion(1381, 710, 250, 96).click(); yield()
                // Click close
                region.waitHas(FileTemplate("close.png"), 15000)?.click()
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
        if (region.doesntHave(FileTemplate("locations/landmarks/combat_menu.png"))) {
            logger.info("Chapter menu not on screen!")
            // Click back button in case one of the maps was opened accidentally
            region.subRegion(383, 92, 169, 94).click()
        }
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
        // Only needed when script first starts/just restarted as we are unsure what map mode the game is in
        if (scriptRunner.justRestarted) {
            when {
                map.endsWith("e", true) -> {
                    logger.info("Selecting emergency map")
                    region.subRegion(1700, 265, 142, 50).click()
                }
                map.endsWith("n", true) -> {
                    logger.info("Selecting night map")
                    region.subRegion(1871, 265, 142, 50).click()
                }
                else -> {
                    // Normal map
                    region.subRegion(1528, 265, 142, 50).click()
                }
            }
            // Wait for it to settle
            delay(400)
        }
        navigator.checkLogistics()

        // Swipe up if map is > 4
        when (val mapNum = map.drop(2).take(1).toInt()) {
            in 1..4 -> {
                region.subRegion(925, 377 + 177 * (mapNum - 1), 440, 130).click()
            }
            else -> {
                val mapNameR = region.subRegion(1100, 350, 250, 680)
                val mapName = map.dropLastWhile { it.isLetter() }
                withTimeout(10000) {
                    while (isActive) {
                        region.subRegion(1020, 880, 675, 140).randomPoint().let {
                            region.device.input.touchInterface?.swipe(
                                    ITouchInterface.Swipe(0, it.x, it.y, it.x, it.y - 650), 1000
                            )
                        }
                        if (Ocr.forConfig(config).doOCRAndTrim(mapNameR).contains(mapName)) break
                        yield()
                    }
                }
                navigator.checkLogistics()
                region.subRegion(925, 472 + 177 * (mapNum - 4), 440, 130).click()
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
                    .findBest(FileTemplate("combat/battle/normal.png"))?.region?.click() ?: continue
            delay(200)

            if (checkNeedsEnhancement()) return

            // Wait for start operation button to appear first before handing off control to
            // map specific files
            if (region.waitHas(FileTemplate("combat/battle/start.png"), 15000) != null) {
                logger.info("Entered map $map")
                break
            }
        }

        // Set location to battle
        gameState.currentGameLocation = GameLocation(LocationId.BATTLE)
    }

    /**
     * Checks if the enhancement dialog popped up
     */
    private fun checkNeedsEnhancement(): Boolean {
        val r = region.subRegion(1185, 696, 278, 95)
        r.findBest(FileTemplate("combat/tdoll-enhance.png"))?.region?.apply {
            logger.info("T-doll limit reached, cancelling sortie")
            click()
            gameState.dollOverflow = true
            gameState.currentGameLocation = GameLocation.mappings(config)[LocationId.TDOLL_ENHANCEMENT]
                    ?: error("Bad locations.json file")
            return true
        }
        r.findBest(FileTemplate("combat/equip-enhance.png"))?.region?.apply {
            logger.info("Equipment limit reached, cancelling sortie")
            click()
            gameState.equipOverflow = true
            gameState.currentGameLocation = GameLocation.mappings(config)[LocationId.RESEARCH_MENU]
                    ?: error("Bad locations.json file")
            return true
        }
        return false
    }

    /**
     * Sets the similarity threshold to map runner settings then executes the map runner,
     * it restores the default similarity threshold before finishing
     */
    private suspend fun executeMapRunner() {
        // Set similarity to slightly lower threshold for discrepancies because of zoom level
        region.matcher.settings.defaultThreshold = config.scriptConfig.mapRunnerSimilarityThreshold
        mapRunner.execute()
        // Restore script threshold
        region.matcher.settings.defaultThreshold = config.scriptConfig.defaultSimilarityThreshold
    }
    //</editor-fold>
}
