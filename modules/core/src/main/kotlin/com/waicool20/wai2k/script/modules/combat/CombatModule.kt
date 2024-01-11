/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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

import com.waicool20.cvauto.core.input.ITouchInterface
import com.waicool20.cvauto.core.template.FT
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.core.template.ImageTemplate
import com.waicool20.cvauto.core.util.countColor
import com.waicool20.cvauto.core.util.isSimilar
import com.waicool20.cvauto.core.util.pipeline
import com.waicool20.wai2k.game.CombatMap
import com.waicool20.wai2k.game.TDoll
import com.waicool20.wai2k.game.location.GameLocation
import com.waicool20.wai2k.game.location.LocationId
import com.waicool20.wai2k.script.*
import com.waicool20.wai2k.script.modules.ScriptModule
import com.waicool20.wai2k.util.disableDictionaries
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.wai2k.util.readText
import kotlinx.coroutines.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.text.DecimalFormat
import kotlin.reflect.full.primaryConstructor

@Suppress("unused")
class CombatModule(navigator: Navigator) : ScriptModule(navigator) {
    private val logger = loggerFor<CombatModule>()

    private val map by lazy {
        MapRunner.list.keys.find { it.name == profile.combat.map }
            ?: throw UnsupportedMapException(profile.combat.map)
    }

    private val mapRunner by lazy {
        MapRunner.list[map]!!.primaryConstructor!!.call(this)
    }

    private var wasCancelled = false

    override suspend fun execute() {
        if (!profile.combat.enabled) return
        // Return if the base doll limit is already reached
        if (gameState.dollOverflow) return
        // Return if the equip limit is already reached
        if (gameState.equipOverflow) return
        if (map is CombatMap.StoryMap) {
            runCombatCycle()
        } else {
            runOtherCombatCycle()
        }
    }

    /**
     * Runs a combat cycle
     */

    private suspend fun runCombatCycle() {
        // Don't need to switch dolls if previous run was cancelled
        // or the map is not meant for corpse dragging
        if (mapRunner is CorpseDragging && !wasCancelled) {
            switchDolls()
            // Check if there was a bad switch
            if (wasCancelled) {
                logger.info("Bad switch, maybe the doll positions got shifted, cancelling this run")
                return
            }
        }
        // Cancel further execution if any of the dolls needed to repair but were not able to
        wasCancelled = gameState.echelons.any { it.needsRepairs() }
        if (wasCancelled) return

        navigator.navigateTo(LocationId.COMBAT)
        val map = map as CombatMap.StoryMap
        clickCombatChapter(map); delay(1000)
        clickCombatMap(map); delay(1000)
        enterBattle(map); delay(1000)
        // Cancel further execution if not in battle, maybe due to doll/equip overflow
        wasCancelled = gameState.currentGameLocation.id != LocationId.BATTLE
        if (wasCancelled) return

        executeMapRunner()

        // Set game location back to combat menu now that battle has ended
        gameState.currentGameLocation = GameLocation.find(config, LocationId.COMBAT_MENU)
        logger.info("Sortie complete")
        // Back to combat menu or home, check logistics
        navigator.checkLogistics()
    }

    private suspend fun runOtherCombatCycle() {
        if (mapRunner is CorpseDragging && !wasCancelled) {
            switchDolls()
            // Check if there was a bad switch
            if (wasCancelled) {
                logger.info("Bad switch, maybe the doll positions got shifted, cancelling this run")
                return
            }
        }
        // Cancel further execution if any of the dolls needed to repair but were not able to
        wasCancelled = gameState.echelons.any { it.needsRepairs() }
        if (wasCancelled) return

        val r = region.subRegion(1472, 882, 448, 198)

        val loc = when (map) {
            is CombatMap.EventMap -> LocationId.EVENT
            is CombatMap.CampaignMap -> LocationId.CAMPAIGN
            else -> error("Expected Event or Campaign map")
        }

        navigator.navigateTo(loc)

        (mapRunner as CustomMapEntrance).enterMap()
        delay(1000)

        if (checkNeedsEnhancement()) return
        // Wait for start operation button to appear first before handing off control to
        // map specific files
        if (r.waitHas(FileTemplate("combat/battle/start.png", 0.9), 30000) == null) {
            wasCancelled = true
            return
        } else {
            logger.info("Entered $loc map")
            executeMapRunner()
        }

        gameState.currentGameLocation = GameLocation.find(config, loc)
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

        val slot = profile.combat.draggerSlot

        val tdoll = run {
            val tdolls = profile.combat.draggers
                .map { TDoll.lookup(config, it.id) ?: throw InvalidDollException(it.id) }
            if (tdolls[0] == tdolls[1]) {
                tdolls.first()
            } else {
                tdolls.first { it.name != gameState.echelons[0].members[slot - 1].name }
            }
        }

        if (gameState.switchDolls) {
            val startTime = System.currentTimeMillis()
            logger.info("Switching doll $slot of echelon 1")
            // Dragger region ( excludes stuff below name/type )
            region.subRegion(237 + (slot - 1) * 273, 206, 247, 544).click(); yield()
            region.waitHas(FileTemplate("doll-list/lock.png"), 5000)

            applyFilters(tdoll, false)
            var switchDoll = region.findBest(FileTemplate("doll-list/echelon2-captain.png"))?.region

            val r1 = region.subRegion(910, 1038, 500, 20)
            val r2 = r1.copy(y = r1.y - 325)
            val checkRegion = region.subRegion(185, 360, 60, 60)

            var scrollDown = true
            var checkImg: BufferedImage

            try {
                withTimeout(90_000) {
                    val r = region.subRegion(46, 146, 1542, 934)
                    while (coroutineContext.isActive) {
                        if (region.pickColor(0, 300).isSimilar(Color(21, 21, 21))) {
                            // Exit doll profile screen if entered accidentally when emu lags while swiping
                            region.subRegion(120, 597, 132, 88).click()
                            delay(500)
                            continue
                        }
                        // Trying this to improve search reliability, maybe put this upstream in cvauto
                        switchDoll =
                            r.findBest(FileTemplate("doll-list/echelon2-captain.png", 0.85), 5)
                                .maxByOrNull { it.score }?.region
                        if (switchDoll == null) {
                            checkImg = checkRegion.capture().img
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
                }
            } catch (e: TimeoutCancellationException) {
                throw ReplacementDollNotFoundException()
            }

            switchDoll?.copy(width = 142)?.click()
            logger.info("Switching dolls took ${System.currentTimeMillis() - startTime} ms")
            delay(1250)
        }

        updateEchelonRepairStatus(1)

        val echelon1Members = gameState.echelons[0].members.map { it.name }
        wasCancelled =
            profile.combat.draggers.none { TDoll.lookup(config, it.id)?.name in echelon1Members }

        // Sometimes update echelon repair status reads the old dolls name because old doll is still
        // on screen briefly after the switch
        if (scriptStats.sortiesDone >= 1 && gameState.echelons[0].members[slot - 1].name != tdoll.name) {
            logger.warn("Expected new dragger to be ${tdoll.name}, got ${gameState.echelons[0].members[slot - 1].name}")
            gameState.echelons[0].members[slot - 1].name = tdoll.name
        }
    }

    /**
     * Applies the filters ( stars and types ) in formation doll list
     */
    private suspend fun applyFilters(tdoll: TDoll, reset: Boolean) {
        logger.info("Applying doll filters for dragging doll ${tdoll.name}")
        tdoll.apply { applyDollFilters(stars, type, reset) }
        delay(500)
        region.subRegion(1260, 180, 371, 317)
            .waitDoesntHave(FileTemplate("doll-list/filtermenu.png"), 5000)
    }

    //</editor-fold>

    //<editor-fold desc="Repair">

    private suspend fun updateEchelonRepairStatus(echelon: Int, retries: Int = 3) {
        logger.info("Updating repair status")

        // Temporary convenience class for storing doll regions
        class DollRegions(nameImage: BufferedImage, hpImage: BufferedImage) {
            val tdollOcr = ocr.disableDictionaries()
                    .readText(nameImage, threshold = 0.72, invert = true, scale = 0.8)
            val tdoll = TDoll.lookup(config, tdollOcr)
            val percent = run {
                val image = hpImage.pipeline().threshold().toBufferedImage()
                image.countColor(Color.WHITE) / image.width.toDouble() * 100
            }
        }

        for (i in 1..retries) {
            val cache = region.freeze()
            val members = cache.findBest(FileTemplate("formation/stats.png"), 5)
                .also { logger.info("Found ${it.size} dolls on screen") }
                .map { it.region }
                .sortedBy { it.x }
                .map {
                    DollRegions(
                        cache.capture().img.getSubimage(it.x - 153, it.y - 183, 247, 84),
                        cache.capture().img.getSubimage(it.x - 133, it.y - 61, 209, 1)
                    )
                }

            val formatter = DecimalFormat("##.#")
            gameState.echelons[echelon - 1].members.forEachIndexed { j, member ->
                val dMember = members.getOrNull(j)
                member.name = dMember?.tdoll?.name ?: "Unknown"
                member.needsRepair = (dMember?.percent ?: 100.0) < profile.combat.repairThreshold
                val sPercent = dMember?.percent?.let { formatter.format(it) } ?: "N/A"
                logger.info("[Repair OCR] Name: ${dMember?.tdollOcr} | HP (%): $sPercent")
            }

            // Checking if the ocr results were gibberish
            val dragger = members.getOrNull(profile.combat.draggerSlot - 1)?.tdoll?.name
            if (dragger == null || profile.combat.draggers.none { it.id.contains(dragger) }) {
                logger.info("Update repair status ocr failed after $i attempts, retries remaining: ${retries - i}")
                if (i == retries) {
                    logger.warn("Could not update repair status after $retries attempts")
                    logger.warn("Check if you set the right T doll as dragger and slot positions")
                    if (scriptStats.sortiesDone > 1 && members.all { it.tdoll == null }) {
                        wasCancelled = true
                        throw RepairUpdateException()
                    }
                    scriptRunner.stop("Could not update repair status")
                }
                delay(2000)
                continue
            }
            break
        }
        logger.info("Updating repair status complete")
        gameState.echelons[echelon - 1].members.forEachIndexed { i, member ->
            logger.info(
                "[Echelon $echelon member ${i + 1}] " +
                    "Name: ${member.name} | " +
                    "Needs repairs: ${member.needsRepair}" +
                    if (i == profile.combat.draggerSlot - 1) " | Dragger" else ""
            )
        }
    }

    //</editor-fold>

    //<editor-fold desc="Map Selection and Combat">

    /**
     * Clicks the given story map chapter
     */
    private suspend fun clickCombatChapter(map: CombatMap.StoryMap) {
        val chapter = map.chapter
        logger.info("Choosing combat chapter $chapter")
        if (region.doesntHave(FileTemplate("locations/landmarks/combat_menu.png"))) {
            logger.info("Chapter menu not on screen!")
            // Click back button in case one of the maps was opened accidentally
            region.subRegion(324, 92, 169, 94).click()
        }
        clickChapter(chapter)
        logger.info("At combat chapter $chapter")
        navigator.checkLogistics()
    }

    /**
     * Clicks the story map
     */
    private suspend fun clickCombatMap(map: CombatMap.StoryMap) {
        logger.info("Choosing combat map $map")
        navigator.checkLogistics()
        // Only needed when script first starts/just restarted as we are unsure what map mode the game is in
        if (gameState.requiresMapInit) {
            when (map.type) {
                CombatMap.Type.NORMAL -> {
                    // Normal map
                    region.subRegion(1428, 260, 100, 60).click()
                }
                CombatMap.Type.EMERGENCY -> {
                    logger.info("Selecting emergency map")
                    region.subRegion(1599, 260, 100, 60).click()
                }
                CombatMap.Type.NIGHT -> {
                    logger.info("Selecting night map")
                    region.subRegion(1765, 260, 100, 60).click()
                }
            }
            // Wait for it to settle
            delay(400)
        }

        // Swipe up if map is > 4
        when (map.number) {
            in 1..4 -> {
                region.subRegion(925, 377 + 177 * (map.number - 1), 440, 130).click()
            }
            else -> {
                val mapNameR = region.subRegion(970, 350, 250, 680)
                val mapName = "${map.chapter}-${map.number}" // map.name might have suffix
                withTimeout(10000) {
                    while (coroutineContext.isActive) {
                        region.subRegion(1020, 880, 675, 140).randomPoint().let {
                            region.device.input.touchInterface.swipe(
                                ITouchInterface.Swipe(0, it.x, it.y, it.x, it.y - 650), 1000
                            )
                        }
                        if (ocr.readText(mapNameR, threshold = 0.6, invert = true)
                                .contains(mapName)
                        ) break
                        yield()
                    }
                }
                region.subRegion(925, 472 + 177 * (map.number - 4), 440, 130).click()
            }
        }
    }

    /**
     * Enters the map and waits for the start button to appear
     */
    private suspend fun enterBattle(map: CombatMap.StoryMap) {
        // Enter battle, use higher similarity threshold to exclude possibly disabled
        // button which will be slightly transparent
        logger.info("Entering normal battle at $map")

        region.waitHas(FT("combat/battle/normal.png", 0.85), 5000)?.click()
            ?: throw ScriptException("Failed to enter normal battle, could not click normal battle button")

        delay(1000)

        if (checkNeedsEnhancement()) return

        val r = region.subRegion(1472, 871, 436, 205)
        // Wait for start operation button to appear first before handing off control to
        // map specific files
        if (r.waitHas(FT("combat/battle/start.png", 0.85), 30000) == null) {
            throw ScriptException("Failed to enter normal battle, start operation button didn't appear after 30s")
        }

        logger.info("Entered map $map")
        // Set location to battle
        gameState.currentGameLocation = GameLocation(LocationId.BATTLE)
    }

    /**
     * Checks if the enhancement dialog popped up
     */
    private suspend fun checkNeedsEnhancement(): Boolean {
        val text = ocr.readText(region.subRegion(942, 463, 276, 63), pad = 0)
        when {
            text.contains("retire", true) -> {
                logger.info("T-doll limit reached, cancelling sortie")
                if (profile.factory.enhancement.enabled) {
                    region.subRegion(1206, 463, 276, 350).click()
                } else {
                    region.subRegion(822, 463, 276, 350).click()
                }
                delay(5000)
                gameState.dollOverflow = true
                gameState.currentGameLocation = GameLocation.find(config, LocationId.FACTORY_MENU)
                return true
            }
            text.contains("dismantle", true) -> {
                logger.info("Equipment limit reached, cancelling sortie")
                region.subRegion(822, 463, 276, 350).click()
                delay(5000)
                gameState.equipOverflow = true
                gameState.currentGameLocation = GameLocation.find(config, LocationId.FACTORY_MENU)
                return true
            }
        }
        logger.info("No limit pop-up, continuing sortie")
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
