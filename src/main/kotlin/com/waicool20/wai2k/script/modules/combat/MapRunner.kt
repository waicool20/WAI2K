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

import boofcv.struct.image.GrayF32
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.util.*
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.game.MapRunnerRegions
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.binarizeImage
import com.waicool20.waicoolutils.countColor
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.pad
import georegression.struct.homography.Homography2D_F64
import kotlinx.coroutines.*
import org.reflections.Reflections
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random


abstract class MapRunner(
        protected val scriptRunner: ScriptRunner,
        protected val region: AndroidRegion,
        protected val config: Wai2KConfig,
        protected val profile: Wai2KProfile
) : CoroutineScope {
    private val logger = loggerFor<MapRunner>()
    private var _battles = 1
    /**
     * Map homography cache
     */
    private var mapH: Homography2D_F64? = null
    /**
     * How many homography samples to take
     */
    private val hSamples = 1
    private val minScroll = 75

    companion object {
        private val mapper = jacksonObjectMapper()

        val list = Reflections("com.waicool20.wai2k.script.modules.combat.maps")
                .getSubTypesOf(MapRunner::class.java)
                .mapNotNull { cls ->
                    Regex("Map(\\d_\\d\\w?)").matchEntire(cls.simpleName)?.let {
                        it.groupValues[1].replace("_", "-") to cls.kotlin
                    }
                }.toMap()
    }

    override val coroutineContext: CoroutineContext
        get() = scriptRunner.coroutineContext

    val gameState get() = scriptRunner.gameState
    val scriptStats get() = scriptRunner.scriptStats

    /**
     * A property that contains the asset prefix of the map
     */
    val PREFIX = "combat/maps/${javaClass.simpleName.replace("_", "-").drop(3)}"

    private val _nodes = async(Dispatchers.IO) {
        mapper.readValue<List<MapNode>>(config.assetsDirectory.resolve("$PREFIX/map.json").toFile())
    }

    private val _fullMap = async(Dispatchers.IO) {
        ImageIO.read(config.assetsDirectory.resolve("$PREFIX/map.png").toFile()).extractNodes()
    }

    /**
     * The nodes defined for this map
     */
    protected val nodes = runBlocking { _nodes.await() }

    protected val fullMap = runBlocking { _fullMap.await() }

    /**
     * Container class that contains commonly used regions
     */
    val mapRunnerRegions = MapRunnerRegions(region)

    /**
     * No. of battles that have passed
     */
    val battles get() = _battles

    /**
     * Set to true to signify the map is a map used for corpse dragging, setting it to false
     * will disable the doll switching
     */
    abstract val isCorpseDraggingMap: Boolean

    /**
     * Main execution function that is executed when map is entered
     */
    abstract suspend fun execute()

    /**
     * Deploys the given echelons to the given locations using click regions
     *
     * @param nodes Nodes to deploy to
     *
     * @return Deployments that need resupply, can be used in conjunction with [resupplyEchelons]
     */
    protected suspend fun deployEchelons(vararg nodes: MapNode): Array<MapNode> = coroutineScope {
        val needsResupply = nodes.filterIndexed { i, node ->
            logger.info("Deploying echelon ${i + 1} to $node")
            node.findRegion().click(); delay(750)
            val screenshot = region.capture()
            val ammoNeedsSupply = async {
                val image = screenshot.getSubimage(645, 820, 218, 1).binarizeImage()
                image.countColor(Color.WHITE) != image.width
            }
            val rationNeedsSupply = async {
                val image = screenshot.getSubimage(645, 860, 218, 1).binarizeImage()
                image.countColor(Color.WHITE) != image.width
            }
            mapRunnerRegions.deploy.click()
            delay(300)
            ammoNeedsSupply.await() && rationNeedsSupply.await()
        }
        needsResupply.forEach { logger.info("Echelon at $it needs resupply!") }
        delay(200)
        logger.info("Deployment complete")
        needsResupply.toTypedArray()
    }

    @JvmName("resupplyEchelonsArray")
    protected suspend fun resupplyEchelons(nodes: Array<MapNode>) = resupplyEchelons(*nodes)

    /**
     * Resupplies an echelon at the given nodes, skips normal type nodes
     *
     * @param nodes Nodes to resupply
     */
    protected suspend fun resupplyEchelons(vararg nodes: MapNode) {
        for (node in nodes.distinct()) {
            if (node.type == MapNode.Type.Normal) continue
            logger.info("Resupplying echelon at $node")
            // Clicking twice, first to highlight the echelon, the second time to enter the deployment menu
            logger.info("Selecting echelon")
            node.findRegion().apply {
                click(); yield()
                click(); delay(300)
            }
            logger.info("Resupplying")
            mapRunnerRegions.resupply.click()
            logger.info("Resupply complete")
            delay(750)
        }
    }

    /**
     * Waits for the G&K splash animation that appears at the beginning of the turn to appear
     * and waits for it to disappear
     *
     * @param timeout Max amount of time to wait for splash, can be set to longer lengths for
     * between turns
     */
    protected suspend fun waitForGNKSplash(timeout: Long = 10000) = coroutineScope {
        logger.info("Waiting for G&K splash screen")
        val battleClicker = launch {
            while (isActive) {
                if (isInBattle()) clickThroughBattle() else yield()
            }
        }
        // Wait for the G&K splash to appear within 10 seconds
        region.matcher.settings.matchDimension = ScriptRunner.HIGH_RES
        region.waitHas(FileTemplate("combat/battle/splash.png"), timeout)?.apply {
            logger.info("G&K splash screen appeared")
            delay(2000)
        } ?: logger.info("G&K splash screen did not appear")
        region.matcher.settings.matchDimension = ScriptRunner.NORMAL_RES
        battleClicker.cancel()
    }

    /**
     * Waits for the current turn to end by counting the amount of battles that have passed
     * then ends the turn. This also clicks through any battle results when node battle ends
     *
     * @param battles Amount of battles expected in this turn
     */
    protected suspend fun waitForTurnEnd(battles: Int) {
        logger.info("Waiting for turn to end, expected battles: $battles")
        var battlesPassed = 0
        while (isActive && battlesPassed < battles) {
            if (isInBattle()) {
                clickThroughBattle()
                battlesPassed++
            }
            yield()
        }
        region.waitHas(FileTemplate("combat/battle/terminate.png"), 1200)
        logger.info("Turn ended")
        endTurn()
    }

    /**
     * Waits for the current turn to end by checking the current turn and amount of action points left
     * Relies on OCR, may not be that reliable
     * This also clicks through any battle results when node battle ends
     *
     * @param turn Turn number
     * @param points No of action points
     */
    protected suspend fun waitForTurnAndPoints(turn: Int, points: Int) {
        logger.info("Waiting for turn $turn and action points $points")
        val ocr = Ocr.forConfig(config, digitsOnly = true)
        var currentTurn = 0
        var currentPoints = 0
        while (isActive && (currentTurn != turn || currentPoints != points)) {
            if (isInBattle()) clickThroughBattle()
            val screenshot = region.capture()
            val newTurn = ocr.doOCRAndTrim(screenshot.getSubimage(748, 53, 86, 72))
                    .let { if (it.firstOrNull() == '8') it.replaceFirst("8", "0") else it }
                    .toIntOrNull() ?: continue
            val newPoints = ocr.doOCRAndTrim(screenshot.getSubimage(1730, 970, 135, 76).binarizeImage().pad(10, 10, Color.BLACK))
                    .toIntOrNull() ?: continue
            // Ignore point deltas larger than 10
            if ((currentTurn != newTurn || currentPoints != newPoints) && abs(currentPoints - newPoints) < 10) {
                logger.info("Current turn: $newTurn ($turn) | Current action points: $newPoints ($points)")
                currentTurn = newTurn
                currentPoints = newPoints
            }
            yield()
        }
        logger.info("Reached required turns and action points!")
        delay(1000)
        while (isActive) {
            if (isInBattle()) clickThroughBattle()
            if (region.has(FileTemplate("combat/battle/terminate.png"))) break
        }
        endTurn()
    }

    /**
     * Waits for the assets to appear and assumes that the turn is complete
     */
    protected suspend fun waitForTurnAssets(vararg assets: String) {
        logger.info("Waiting for ${assets.size} assets to appear")
        while (assets.any { region.doesntHave(FileTemplate(it, 0.98)) }) {
            if (isInBattle()) clickThroughBattle()
            yield()
        }
        logger.info("All assets are now on screen")
        region.waitHas(FileTemplate("combat/battle/terminate.png"), 1200)
        endTurn()
    }

    /**
     * Clicks through the battle results and waits for the game to return to the combat menu
     */
    protected suspend fun handleBattleResults() = coroutineScope {
        logger.info("Battle ended, clicking through battle results")
        val combatMenu = GameLocation.mappings(config)[LocationId.COMBAT_MENU]!!
        val clickLocation = mapRunnerRegions.battleEndClick.randomPoint()
        val clickJob = launch {
            while (isActive) region.device.input.touchInterface?.tap(0, clickLocation.x, clickLocation.y)
        }
        while (isActive) {
            if (combatMenu.isInRegion(region)) break
        }
        clickJob.cancel()
        logger.info("Back at combat menu")
        scriptStats.sortiesDone += 1
        _battles = 1
        mapH = null
    }

    protected suspend fun MapNode.findRegion(): AndroidRegion {
        val window = mapRunnerRegions.window
        val H = mapH ?: fullMap.homographyMultiSample(window.capture().extractNodes())
        // Rect that is relative to the window
        val rect = H.transformRect(rect)
        logger.debug("$this estimated to be at Rect(x=${rect.x}, y=${rect.y}, width=${rect.width}, height=${rect.height})")
        val diff = (rect.width.toDouble() - width).pow(2) + (rect.height.toDouble() - height).pow(2)
        if (diff > 80 * 80) {
            logger.debug("Estimation seems off, will try again after zoom | diff=$diff")
            region.pinch(
                    Random.nextInt(500, 700),
                    Random.nextInt(20, 50),
                    0.0,
                    500
            )
            delay(1500)
            mapH = null
            return findRegion()
        }
        val clickRegion = window.copyAs<AndroidRegion>(
                window.x + rect.x,
                window.y + rect.y,
                rect.width,
                rect.height
        )
        if (!window.contains(clickRegion)) {
            logger.info("Node $this not in map window")
            val center = region.subRegion(
                    (region.width - 5) / 2,
                    (region.height - 5) / 2,
                    5, 5
            )
            when {
                clickRegion.y < window.y -> {
                    val dist = max(window.y - clickRegion.y, minScroll)
                    val from = center.copyAs<AndroidRegion>().apply { translate(0, -dist) }
                    val to = center.copyAs<AndroidRegion>().apply { translate(0, dist) }
                    logger.info("Scroll up $dist px")
                    from.swipeTo(to)
                }
                clickRegion.y > window.y + window.height -> {
                    val dist = max(clickRegion.y - (window.y + window.height), minScroll)
                    val from = center.copyAs<AndroidRegion>().apply { translate(0, dist) }
                    val to = center.copyAs<AndroidRegion>().apply { translate(0, -dist) }
                    logger.info("Scroll down $dist px")
                    from.swipeTo(to)
                }
            }
            when {
                clickRegion.x < window.x -> {
                    val dist = max(window.x - clickRegion.x, minScroll)
                    val from = center.copyAs<AndroidRegion>().apply { translate(-dist, 0) }
                    val to = center.copyAs<AndroidRegion>().apply { translate(dist, 0) }
                    logger.info("Scroll left $dist px")
                    from.swipeTo(to)
                }
                clickRegion.x > window.x + window.width -> {
                    val dist = max(clickRegion.x - (window.x + window.width), minScroll)
                    val from = center.copyAs<AndroidRegion>().apply { translate(dist, 0) }
                    val to = center.copyAs<AndroidRegion>().apply { translate(-dist, 0) }
                    logger.info("Scroll right $dist px")
                    from.swipeTo(to)
                }
            }
            mapH = null
            delay(200)
            return findRegion()
        }
        return clickRegion
    }

    private suspend fun clickThroughBattle() {
        logger.info("Entered battle $_battles")
        // Wait until it disappears
        while (isActive && isInBattle()) yield()
        logger.info("Battle ${_battles++} complete, clicking through battle results")
        delay(400)
        val l = mapRunnerRegions.battleEndClick.randomPoint()
        repeat(Random.nextInt(7, 9)) {
            region.device.input.touchInterface?.tap(0, l.x, l.y); yield()
        }
    }

    private fun isInBattle() = mapRunnerRegions.pauseButton.has(FileTemplate("combat/battle/pause.png", 0.9))

    private suspend fun endTurn() {
        do {
            repeat(Random.nextInt(2, 3)) { mapRunnerRegions.endBattle.click() }
            delay(250)
        } while (region.has(FileTemplate("combat/battle/terminate.png")))
    }

    /**
     * Returns a masked image where nodes and path lines are located
     */
    private fun BufferedImage.extractNodes(): GrayF32 {
        val hsv = asPlanar().asHsv()
        val whiteNodes = hsv.clone().apply { hsvFilter(satRange = 0..10, valRange = 200..255) }.getBand(2)
        val redNodes = hsv.clone().apply { hsvFilter(hueRange = arrayOf(0..10, 350..360), satRange = arrayOf(20..100)) }.getBand(2)
        val blueNodes = hsv.apply { hsvFilter(hueRange = 200..210, satRange = 20..100) }.getBand(2)
        return whiteNodes + redNodes + blueNodes
    }

    /**
     * Calculates multiple homography matrices and then takes an element wise average
     */
    private suspend fun GrayF32.homographyMultiSample(other: GrayF32): Homography2D_F64 {
        return coroutineScope {
            val hDeferred = List(hSamples) { async { homography(other) } }
            val h = hDeferred.map { it.await() }
            val result = Homography2D_F64()
            for (r in 0 until 3) {
                for (c in 0 until 3) {
                    result.set(r, c, h.map { it.get(r, c) }.average())
                }
            }
            result
        }
    }
}