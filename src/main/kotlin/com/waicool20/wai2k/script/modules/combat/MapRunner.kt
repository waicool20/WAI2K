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
import com.waicool20.cvauto.core.asCachedRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.util.homography
import com.waicool20.cvauto.util.transformRect
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.CombatMap
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.game.MapRunnerRegions
import com.waicool20.wai2k.script.NodeNotFoundException
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.ScriptTimeOutException
import com.waicool20.wai2k.script.modules.combat.maps.EventMapRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.wai2k.util.extractNodes
import com.waicool20.waicoolutils.binarizeImage
import com.waicool20.waicoolutils.countColor
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.pad
import georegression.struct.homography.Homography2D_F64
import kotlinx.coroutines.*
import org.reflections.Reflections
import java.awt.Color
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.text.DecimalFormat
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.KClass


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

    companion object {
        private val mapper = jacksonObjectMapper()

        /**
         * Minimum scroll in pixels, because sometimes smaller scrolls dont register properly
         */
        private const val minScroll = 75

        /**
         * Difference theresholds
         */
        private const val maxMapDiff = 80.0
        private const val maxSideDiff = 5.0

        val list = mutableMapOf<CombatMap, KClass<out MapRunner>>()

        init {
            val mapClasses = Reflections("com.waicool20.wai2k.script.modules.combat.maps")
                .getSubTypesOf(MapRunner::class.java)
            for (mapClass in mapClasses) {
                if (Modifier.isAbstract(mapClass.modifiers)) continue
                if (EventMapRunner::class.java.isAssignableFrom(mapClass)) {
                    val name = mapClass.simpleName.replaceFirst("Event", "")
                    list[CombatMap.EventMap(name)] = mapClass.kotlin
                } else {
                    val name = mapClass.simpleName.replaceFirst("Map", "")
                        .replace("_", "-")
                    list[CombatMap.StoryMap(name)] = mapClass.kotlin
                }
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = scriptRunner.coroutineContext

    val gameState get() = scriptRunner.gameState
    val scriptStats get() = scriptRunner.scriptStats

    /**
     * A property that contains the asset prefix of the map
     */
    val PREFIX = "combat/maps/${javaClass.simpleName.replace("_", "-").replace("Map", "")}"

    protected open val extractBlueNodes: Boolean = true
    protected open val extractWhiteNodes: Boolean = false
    protected open val extractYellowNodes: Boolean = true
    protected open val battleTimeout = 45000L // make this a user config?

    private val _nodes = async(Dispatchers.IO) {
        val relPath = config.assetsDirectory.resolve("$PREFIX/map.json")
        val absPath = config.assetsDirectory.resolve("$PREFIX/map-abs.json")
        when {
            Files.exists(relPath) -> mapper.readValue<List<MapNode.RelativeMapNode>>(relPath.toFile())
            Files.exists(absPath) -> mapper.readValue<List<MapNode.AbsoluteMapNode>>(absPath.toFile())
            else -> emptyList()
        }
    }

    private val _fullMap = async(Dispatchers.IO) {
        val path = config.assetsDirectory.resolve("$PREFIX/map.png")
        if (Files.exists(path)) {
            ImageIO.read(path.toFile()).extractNodes(extractBlueNodes, extractWhiteNodes, extractYellowNodes)
        } else {
            GrayF32()
        }
    }

    /**
     * The nodes defined for this map
     */
    protected val nodes = runBlocking { _nodes.await() }

    protected val fullMap = runBlocking { _fullMap.await() }

    /**
     * Set this to true to exit waitFor- Functions early
     */
    protected var interruptWaitFlag = false

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
     * Executes when entering a battle
     */
    protected open suspend fun onEnterBattleListener() = Unit

    /**
     * Executes when finishing a battle
     */
    protected open suspend fun onFinishBattleListener() = Unit

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
            openEchelon(node, singleClick = true)
            val screenshot = region.capture()
            val formatter = DecimalFormat("##.#")

            fun hasMember(mIndex: Int): Boolean {
                val hpText = Ocr.forConfig(config).doOCR(screenshot.getSubimage(404 + mIndex * 272, 751, 40, 22))
                return hpText.contains("hp", true)
            }

            val hasMember2 = hasMember(1)
            val ammoNeedsSupply = async {
                if (!hasMember2) return@async false
                val image = screenshot.getSubimage(645, 820, 218, 1).binarizeImage()
                val ammoCount = image.countColor(Color.WHITE) / image.width.toDouble() * 100
                logger.info("Second member ammo: ${formatter.format(ammoCount)} %")
                image.countColor(Color.WHITE) != image.width
            }
            val rationNeedsSupply = async {
                if (!hasMember2) return@async false
                val image = screenshot.getSubimage(645, 860, 218, 1).binarizeImage()
                val rationCount = image.countColor(Color.WHITE) / image.width.toDouble() * 100
                logger.info("Second member rations: ${formatter.format(rationCount)} %")
                image.countColor(Color.WHITE) != image.width
            }
            if (!isCorpseDraggingMap) {
                for (mIndex in 0..5) {
                    if (!hasMember(mIndex)) continue
                    val hpImage = screenshot.getSubimage(373 + mIndex * 272, 778, 217, 1).binarizeImage()
                    val hp = hpImage.countColor(Color.WHITE) / hpImage.width.toDouble() * 100
                    logger.info("Member ${mIndex + 1} HP: ${formatter.format(hp)} %")
                    if (hp < profile.combat.repairThreshold) {
                        logger.info("Repairing member ${mIndex + 1}")
                        region.subRegion(360 + mIndex * 272, 286, 246, 323).click()
                        region.subRegion(1360, 702, 290, 117)
                            .waitHas(FileTemplate("ok.png"), 3000)?.click()
                        scriptStats.repairs++
                    }
                }
                delay(500)
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
            logger.info("Selecting echelon")
            openEchelon(node)
            logger.info("Resupplying")
            mapRunnerRegions.resupply.click()
            logger.info("Resupply complete")
            delay(750)
        }
    }

    @JvmName("retreatEchelonsArray")
    protected suspend fun retreatEchelons(nodes: Array<MapNode>) = retreatEchelons(*nodes)

    /**
     * Retreats an echelon at the given nodes, skips normal type nodes
     *
     * @param nodes Nodes to retreat
     */
    protected suspend fun retreatEchelons(vararg nodes: MapNode) {
        for (node in nodes.distinct()) {
            if (node.type == MapNode.Type.Normal) continue
            logger.info("Retreat echelon at $node")
            logger.info("Selecting echelon")
            openEchelon(node)
            logger.info("Retreating")
            mapRunnerRegions.retreat.click()
            delay(1000)
            region.subRegion(1115, 696, 250, 95).click()
            logger.info("Retreat complete")
            delay(1000)
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
        val start = System.currentTimeMillis()
        // Wait for the G&K splash to appear within 10 seconds
        while (isActive) {
            delay(500)
            if (mapRunnerRegions.endBattle.has(FileTemplate("combat/battle/end.png", 0.8))) {
                logger.info("G&K splash screen appeared")
                delay(2000)
                break
            }
            if (System.currentTimeMillis() - start > timeout) {
                logger.info("G&K splash screen did not appear")
                break
            }
            if (isInBattle()) clickThroughBattle()
        }
    }

    /**
     * Waits for the current turn to end by counting the amount of battles that have passed
     * then ends the turn. This also clicks through any battle results when node battle ends
     *
     * @param battles Amount of battles expected in this turn
     */
    protected suspend fun waitForTurnEnd(battles: Int, endTurn: Boolean = true) {
        logger.info("Waiting for turn to end, expected battles: $battles")
        var battlesPassed = 0
        try {
            withTimeout(battles * battleTimeout) {
                while (isActive && battlesPassed < battles) {
                    if (isInBattle()) {
                        clickThroughBattle()
                        battlesPassed++
                    }
                    yield()
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw ScriptTimeOutException("Waiting for battles", e)
        } finally {
            _battles = 1
            mapH = null
        }
        region.waitHas(FileTemplate("combat/battle/terminate.png"), 10000)
        logger.info("Turn ended")
        if (endTurn) endTurn()
    }

    /**
     * Waits for the current turn to end by checking the current turn and amount of action points left
     * Relies on OCR, may not be that reliable
     * This also clicks through any battle results when node battle ends
     *
     * @param turn Turn number
     * @param points No of action points
     */
    protected suspend fun waitForTurnAndPoints(turn: Int, points: Int, endTurn: Boolean = true) = coroutineScope {
        logger.info("Waiting for turn $turn and action points $points")
        val ocr = Ocr.forConfig(config, digitsOnly = true)
        var currentTurn = 0
        var currentPoints = 0
        try {
            withTimeout(120_000) {
                loop@ while (isActive && !interruptWaitFlag) {
                    delay(500)
                    when {
                        isInBattle() -> clickThroughBattle()
                        currentTurn == turn && currentPoints == points -> break@loop
                    }
                    val screenshot = region.capture()
                    val newTurn = ocr.doOCRAndTrim(screenshot.getSubimage(748, 53, 86, 72))
                        .let { if (it.firstOrNull() == '8') it.replaceFirst("8", "0") else it }
                        .toIntOrNull() ?: continue


                    val newPoints = ocr.doOCRAndTrim(screenshot.getSubimage(1730, 970, 135, 76)
                        .binarizeImage().pad(10, 10, Color.BLACK))
                        .toIntOrNull() ?: continue


                    // Ignore point deltas larger than 10
                    if ((currentTurn != newTurn || currentPoints != newPoints) && abs(currentPoints - newPoints) < 10) {
                        logger.info("Current turn: $newTurn ($turn) | Current action points: $newPoints ($points)")
                        currentTurn = newTurn
                        currentPoints = newPoints
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw ScriptTimeOutException("Waiting for turn and points", e)
        } finally {
            _battles = 1
            mapH = null
        }
        if (interruptWaitFlag) {
            logger.info("Aborting Wait...")
        } else {
            logger.info("Reached required turns and action points!")
        }
        delay(1000)
        while (isActive) {
            if (isInBattle()) clickThroughBattle()
            if (region.has(FileTemplate("combat/battle/terminate.png"))) break
        }
        if (endTurn) endTurn()
    }

    /**
     * Waits for the assets to appear and assumes that the turn is complete
     */
    protected suspend fun waitForTurnAssets(endTurn: Boolean = true, threshold: Double = 0.98, vararg assets: String) {
        logger.info("Waiting for ${assets.size} assets to appear:")
        assets.forEach { logger.info("Waiting on: $it") }
        try {
            withTimeout(120_000) {
                while (isActive) {
                    if (isInBattle()) clickThroughBattle()
                    val r = region.asCachedRegion()
                    if (assets.all { r.has(FileTemplate(it, threshold)) }) break
                    yield()
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw ScriptTimeOutException("Waiting for assets", e)
        } finally {
            _battles = 1
            mapH = null
        }
        logger.info("All assets are now on screen")
        region.waitHas(FileTemplate("combat/battle/terminate.png"), 10000)
        if (endTurn) endTurn()
    }

    /**
     * Clicks through the battle results and waits for the game to return to the combat menu
     */
    protected suspend fun handleBattleResults(): Unit = coroutineScope {
        logger.info("Battle ended, clicking through battle results")
        val location = if (this@MapRunner is EventMapRunner) {
            logger.info("Waiting for event menu")
            GameLocation.mappings(config)[LocationId.EVENT]
        } else {
            logger.info("Waiting for combat menu")
            GameLocation.mappings(config)[LocationId.COMBAT_MENU]
        }
        checkNotNull(location)

        try {
            withTimeout(60000) {
                var extraClicks = 10.0
                while (!location.isInRegion(region)) {
                    // Speed this up as time goes on
                    repeat((extraClicks / 10).roundToInt()) {
                        mapRunnerRegions.battleEndClick.click()
                        extraClicks += 1
                    }
                    endTurn()
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw ScriptTimeOutException("Waiting to exit battle", e)
        } finally {
            scriptStats.sortiesDone += 1
            _battles = 1
            mapH = null
        }
    }

    protected suspend fun terminateMission() {
        mapRunnerRegions.terminateMenu.click(); delay(700)
        mapRunnerRegions.terminate.click(); delay(5000)

        logger.info("Left battle screen")
        scriptStats.sortiesDone += 1
        _battles = 1
        mapH = null
    }

    protected suspend fun MapNode.findRegion(): AndroidRegion {
        if (this is MapNode.AbsoluteMapNode) return region.subRegionAs(x, y, width, height)
        val window = mapRunnerRegions.window
        var h: Homography2D_F64? = null
        while (h == null) {
            h = try {
                mapH
                    ?: fullMap.homography(window.capture().extractNodes(extractBlueNodes, extractWhiteNodes, extractYellowNodes))
            } catch (e: IllegalStateException) {
                continue
            }
        }

        suspend fun retry(): AndroidRegion {
            if (Random.nextBoolean()) {
                logger.info("Zoom out")
                region.pinch(
                    Random.nextInt(500, 700),
                    Random.nextInt(300, 400),
                    0.0,
                    500
                )
                delay(1000)
            }
            mapH = null
            return findRegion()
        }

        // Rect that is relative to the window
        val rect = h.transformRect(rect)
        logger.debug("$this estimated to be at Rect(x=${rect.x}, y=${rect.y}, width=${rect.width}, height=${rect.height})")
        if (rect.width <= 0 || rect.height <= 0) {
            logger.debug("Estimate failed basic dimension test, will retry")
            return retry()
        }
        val roi = window.copyAs<AndroidRegion>(
            window.x + rect.x,
            window.y + rect.y,
            rect.width,
            rect.height
        )
        // Difference from reference values in map.json and estimated rect values
        val mapDiff = (rect.width.toDouble() - width).pow(2) + (rect.height.toDouble() - height).pow(2)
        if (mapDiff > maxMapDiff.pow(2)) {
            logger.info("Estimate failed map difference test, will retry | diff=$mapDiff, max=$maxMapDiff")
            return retry()
        }
        // Difference between rect width and height, should be roughly square (1:1)
        val sideDiff = (rect.width.toDouble() - rect.height).pow(2)
        if (sideDiff > maxSideDiff.pow(2)) {
            logger.debug("Estimate failed side difference test, will retry | diff=$sideDiff, max=$maxSideDiff")
            return retry()
        }
        if (!window.contains(roi)) {
            logger.info("Node $this not in map window")
            val center = region.subRegion(
                (region.width - 5) / 2,
                (region.height - 5) / 2,
                5, 5
            )
            // Add some randomness
            center.translate(Random.nextInt(-50, 50), Random.nextInt(-50, 50))
            when {
                roi.y < window.y -> {
                    val dist = max(window.y - roi.y, minScroll)
                    val from = center.copyAs<AndroidRegion>().apply { translate(0, -dist) }
                    val to = center.copyAs<AndroidRegion>().apply { translate(0, dist) }
                    logger.info("Scroll up $dist px")
                    from.swipeTo(to)
                }
                roi.y > window.y + window.height -> {
                    val dist = max(roi.y - (window.y + window.height), minScroll)
                    val from = center.copyAs<AndroidRegion>().apply { translate(0, dist) }
                    val to = center.copyAs<AndroidRegion>().apply { translate(0, -dist) }
                    logger.info("Scroll down $dist px")
                    from.swipeTo(to)
                }
            }
            when {
                roi.x < window.x -> {
                    val dist = max(window.x - roi.x, minScroll)
                    val from = center.copyAs<AndroidRegion>().apply { translate(-dist, 0) }
                    val to = center.copyAs<AndroidRegion>().apply { translate(dist, 0) }
                    logger.info("Scroll left $dist px")
                    from.swipeTo(to)
                }
                roi.x > window.x + window.width -> {
                    val dist = max(roi.x - (window.x + window.width), minScroll)
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

        while (isActive) {
            val targets = mutableListOf<Pair<Int, Int>>()
            val img = roi.capture().extractNodes()
            for (y in 0 until img.height) {
                var index = img.startIndex + y * img.stride
                for (x in 0 until img.width) {
                    if (img.data[index++] >= 175) targets += x to y
                }
            }
            yield()
            if (targets.isEmpty()) {
                logger.debug("No targets found, retry")
                if (Random.nextBoolean()) continue else return retry()
            }
            logger.debug("${targets.size} target candidates for node $this")
            val target = targets.random().let { (cX, cY) ->
                region.subRegionAs<AndroidRegion>(roi.x + cX, roi.y + cY, 5, 5)
            }
            logger.debug("Node target: (x=${target.x},y=${target.y})")
            return target
        }
        throw NodeNotFoundException(this)
    }

    private suspend fun clickThroughBattle() {
        logger.info("Entered battle $_battles")
        onEnterBattleListener()
        // Wait until it disappears
        while (isActive && isInBattle()) yield()
        logger.info("Battle ${_battles++} complete, clicking through battle results")
        // Animation and load wheel until you can click through results/drops
        delay(Random.nextLong(1000, 1200))
        val l = mapRunnerRegions.battleEndClick.randomPoint()
        val clicks = if (config.scriptConfig.minPostBattleClick == config.scriptConfig.maxPostBattleClick) {
            config.scriptConfig.minPostBattleClick
        } else {
            Random.nextInt(config.scriptConfig.minPostBattleClick, config.scriptConfig.maxPostBattleClick)
        }
        repeat(clicks) {
            region.subRegion(l.x, l.y, 20, 20).click()
            delay(Random.nextLong(150, 250))
        }
        // If the clicks above managed to halt battle plan just cancel the dialog
        delay(1000)
        region.subRegion(761, 674, 283, 144)
            .findBest(FileTemplate("combat/battle/cancel.png"))?.region?.click()
        onFinishBattleListener()
    }

    private fun isInBattle() = mapRunnerRegions.pauseButton.has(FileTemplate("combat/battle/pause.png", 0.9))

    protected suspend fun openEchelon(node: MapNode, singleClick: Boolean = false) {
        val r = node.findRegion()
        repeat(if (singleClick) 1 else 2) {
            r.click(); delay(1500)
        }
        if (node.type == MapNode.Type.HeavyHeliport && gameState.requiresMapInit) {
            mapRunnerRegions.chooseEchelon.click()
        }
    }

    private suspend fun endTurn() {
        mapRunnerRegions.endBattle.clickWhile { has(FileTemplate("combat/battle/end.png", 0.8)) }
        delay(200)
    }
}