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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.core.template.ITemplate
import com.waicool20.wai2k.game.*
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.ScriptTimeOutException
import com.waicool20.wai2k.util.*
import com.waicool20.waicoolutils.binarizeImage
import com.waicool20.waicoolutils.countColor
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.mapAsync
import com.waicool20.waicoolutils.pad
import kotlinx.coroutines.*
import org.reflections.Reflections
import java.awt.Color
import java.lang.reflect.Modifier
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.reflect.KClass

/**
 * Base class containing most of the scripting framework required to script a map, this includes
 * common actions such as deploying, resupplying and retreating echelons. It also keeps track
 * whenever an echelon enters battle after calling any of the waitFor* functions.
 *
 * The main entry method is [begin] which is called after entering the map, [cleanup] is called
 * after the end of the [begin] method before handing control back to the main script
 * loop. [cleanup] is also called in case any exceptions (e.g Timeouts) occur during a MapRunner cycle.
 *
 * Assets specific to a MapRunner can be put in `/combat/maps/<map-name>`, for
 * convenient access a variable called [PREFIX] is defined for each map.
 */
abstract class MapRunner(
    scriptComponent: ScriptComponent
) : ScriptComponent by scriptComponent, CoroutineScope {

    class Deployment(val echelon: Int, val mapNode: MapNode) : Deployable
    class Retreat(val mapNode: MapNode) : Retreatable

    infix fun Int.at(mapNode: MapNode) = Deployment(this, mapNode)

    companion object {
        val list = mutableMapOf<CombatMap, KClass<out MapRunner>>()

        init {
            val mapClasses = Reflections("com.waicool20.wai2k.script.modules.combat.maps")
                .getSubTypesOf(MapRunner::class.java)
            for (mapClass in mapClasses) {
                if (Modifier.isAbstract(mapClass.modifiers)) continue
                if (Modifier.isInterface(mapClass.modifiers)) continue
                when {
                    CampaignMapRunner::class.java.isAssignableFrom(mapClass) -> {
                        val name = mapClass.simpleName.replaceFirst("Campaign", "")
                            .replace("_", "-")
                        list[CombatMap.CampaignMap(name)] = mapClass.kotlin
                    }
                    EventMapRunner::class.java.isAssignableFrom(mapClass) -> {
                        val name = mapClass.simpleName.replaceFirst("Event", "")
                            .replace("_", "-")
                        list[CombatMap.EventMap(name)] = mapClass.kotlin
                    }
                    else -> {
                        val name = mapClass.simpleName.replaceFirst("Map", "")
                            .replace("_", "-")
                        list[CombatMap.StoryMap(name)] = mapClass.kotlin
                    }
                }
            }
        }
    }

    private val logger = loggerFor<MapRunner>()
    private var _battles = 1

    override val coroutineContext: CoroutineContext
        get() = scriptRunner.coroutineContext

    val gameState get() = scriptRunner.gameState
    val scriptStats get() = scriptRunner.scriptStats

    /**
     * A property that contains the asset prefix of the map
     */
    val PREFIX = "combat/maps/${javaClass.simpleName.replace("_", "-").replace("Map", "")}"

    /**
     * The nodes defined for this map
     */
    abstract val nodes: List<MapNode>

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
     * Dragger ammo resupply threshold, if ammo level is below this level during deployment,
     * the echelon will be resupplied.
     */
    protected open val ammoResupplyThreshold = 1.0

    /**
     * Dragger rations resupply threshold, if rations level is below this level during deployment,
     * the echelon will be resupplied.
     */
    protected open val rationsResupplyThreshold = 0.8

    /**
     * Main run function that goes through whole life cycle of MapRunner
     */
    suspend fun execute() {
        try {
            begin()
        } catch (e: Exception) {
            throw e
        } finally {
            cleanup()
            _battles = 1
            // Only toggle switchDolls true if false, else keep it true
            if (this is CorpseDragging) gameState.switchDolls = true
        }
    }

    /**
     * Function that is executed when map is entered
     */
    abstract suspend fun begin()

    /**
     * Cleanup function run after execute()
     */
    open suspend fun cleanup() = Unit

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
     * @param deployments Either a [MapNode] or [Deployment]
     *
     * @return Deployments that need resupply, can be used in conjunction with [resupplyEchelons]
     */
    protected suspend fun deployEchelons(vararg deployments: Deployable): Array<MapNode> =
        coroutineScope {
            val needsResupply = mutableSetOf<MapNode>()
            deployments.forEachIndexed { i, d ->
                val echelon: Int
                val node: MapNode

                when (d) {
                    is MapNode -> {
                        echelon = -1
                        node = d
                    }
                    is Deployment -> {
                        echelon = d.echelon
                        node = d.mapNode
                    }
                }

                logger.info("Deploying echelon ${i + 1} to $node")
                openEchelon(node)
                if (echelon in 1..10) {
                    while (!clickEchelon(Echelon(echelon))) delay(200)
                }
                val screenshot = region.capture()
                val formatter = DecimalFormat("##.#")

                fun hasMember(mIndex: Int): Boolean {
                    return screenshot.getRGB(385 + mIndex * 272, 765) == Color.WHITE.rgb
                }

                val members = if (this@MapRunner is CorpseDragging) {
                    logger.info("Corpse dragging map, only member 2 will be scanned")
                    listOf(1)
                } else {
                    (0..4).toList()
                }.filter { hasMember(it) }

                val ammoNeedsSupply = async {
                    members.mapAsync { m ->
                        val image =
                            screenshot.getSubimage(373 + m * 272, 820, 218, 1).binarizeImage()
                        val ammoCount = image.countColor(Color.WHITE) / image.width.toDouble()
                        if (ammoCount < ammoResupplyThreshold) needsResupply += node
                        m to ammoCount
                    }.toMap()
                }
                val rationNeedsSupply = async {
                    members.mapAsync { m ->
                        val image =
                            screenshot.getSubimage(373 + m * 272, 860, 218, 1).binarizeImage()
                        val rationCount = image.countColor(Color.WHITE) / image.width.toDouble()
                        if (rationCount < rationsResupplyThreshold) needsResupply += node
                        m to rationCount
                    }.toMap()
                }

                val hpMap = async {
                    members.mapAsync { m ->
                        // Don't need to check health if corpse dragging map because it's already checked
                        // when going to formation
                        if (this@MapRunner is CorpseDragging) {
                            m to -1.0
                        } else {
                            val hpImage =
                                screenshot.getSubimage(373 + m * 272, 778, 217, 1).binarizeImage()
                            m to hpImage.countColor(Color.WHITE) / hpImage.width.toDouble() * 100
                        }
                    }.toMap()
                }

                logger.info("----- Members -----")
                members.forEach { m ->
                    val ammo = formatter.format(ammoNeedsSupply.await()[m]!! * 100)
                    val rations = formatter.format(rationNeedsSupply.await()[m]!! * 100)
                    val hp = hpMap.await()[m]?.takeIf { it in 0.0..100.0 }
                        ?.let { formatter.format(it) } ?: "N/A"
                    logger.info("Member ${m + 1} | HP: $hp%\t\t| Ammo: $ammo%\t\t| Rations: $rations%")
                }
                logger.info("-------------------")

                if (this@MapRunner !is CorpseDragging) {
                    members.forEach { m ->
                        if (hpMap.await()[m]!! in 0.0..profile.combat.repairThreshold.toDouble()) {
                            logger.info("Repairing member ${m + 1}")
                            region.subRegion(360 + m * 272, 228, 246, 323).click()
                            region.subRegion(1441, 772, 250, 96)
                                .waitHas(FileTemplate("ok.png"), 3000)?.click()
                            scriptStats.repairs++
                            delay(500)
                        }
                    }
                }

                mapRunnerRegions.deploy.click()
                delay(1000)
            }
            needsResupply.forEach { logger.info("Echelon at $it needs resupply!") }
            delay(200)
            logger.info("Deployment complete")
            needsResupply.toTypedArray()
        }

    /**
     * Selects an echelon in the deploy list
     *
     * @param echelon echelon number to deploy
     */
    protected suspend fun clickEchelon(echelon: Echelon): Boolean {
        logger.debug("Clicking the echelon")
        val eRegion = region.subRegion(140, 40, 170, region.height - 140)
        delay(100)

        val start = System.currentTimeMillis()
        while (isActive) {
            val echelons = eRegion.findBest(FileTemplate("echelons/echelon.png"), 8)
                .map { it.region }
                .map { it.copy(it.x + 93, it.y - 40, 60, 100) }
                .mapAsync {
                    Ocr.forConfig(config, true).doOCRAndTrim(it)
                        .replace("18", "10").toInt() to it
                }
                .toMap()
            logger.debug("Visible echelons: ${echelons.keys}")
            when {
                echelons.keys.isEmpty() -> {
                    logger.info("No echelons available...")
                    return false
                }
                echelon.number in echelons.keys -> {
                    logger.info("Found echelon!")
                    echelons[echelon.number]?.click()
                    return true
                }
            }
            val lEchelon = echelons.keys.minOrNull() ?: echelons.keys.firstOrNull() ?: continue
            val hEchelon = echelons.keys.maxOrNull() ?: echelons.keys.lastOrNull() ?: continue
            val lEchelonRegion = echelons[lEchelon] ?: continue
            val hEchelonRegion = echelons[hEchelon] ?: continue
            when {
                echelon.number <= lEchelon -> {
                    logger.debug("Swiping down the echelons")
                    lEchelonRegion.swipeTo(hEchelonRegion)
                }
                echelon.number >= hEchelon -> {
                    logger.debug("Swiping up the echelons")
                    hEchelonRegion.swipeTo(lEchelonRegion)
                }
            }
            delay(300)
            if (System.currentTimeMillis() - start > 45000) {
                gameState.requiresUpdate = true
                logger.warn("Failed to find echelon, maybe ocr failed?")
                break
            }
        }
        return false
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
    protected suspend fun retreatEchelons(nodes: Array<Retreatable>) = retreatEchelons(*nodes)

    /**
     * Retreats an echelon at the given nodes, skips normal type nodes
     *
     * @param retreats Nodes to retreat
     */
    protected suspend fun retreatEchelons(vararg retreats: Retreatable) {
        val rl = retreats.distinctBy {
            when (it) {
                is MapNode -> it
                is Retreat -> it.mapNode
            }
        }
        for (retreat in rl) {
            when (retreat) {
                is MapNode -> retreatEchelon(retreat)
                is Retreat -> retreatEchelon(retreat.mapNode)
            }
        }
    }

    private suspend fun retreatEchelon(mapNode: MapNode) {
        if (mapNode.type == MapNode.Type.Normal) return
        logger.info("Retreat echelon at $mapNode")
        logger.info("Selecting echelon")
        openEchelon(mapNode)
        logger.info("Retreating")
        mapRunnerRegions.retreat.click()
        delay(1000)
        region.subRegion(1115, 696, 250, 95).click()
        logger.info("Retreat complete")
        delay(1000)
    }

    /**
     * Swaps two adjacent echelons on the map.
     * May cause map panning if nodes are close to edge of viewport or causes a map event.
     *
     * @param nodes Swaps from first to second
     */
    protected suspend fun swapEchelons(nodes: Pair<MapNode, MapNode>) {
        val (node1, node2) = nodes
        logger.info("Swapping node $node1 with node $node2")
        // If this node or a different adjacent node is already 'selected' may cause issues
        node1.findRegion().click()
        delay(500)
        node2.findRegion().click()
        region.waitHas(FileTemplate("combat/battle/switch.png", 0.95), 2000)?.click()
        delay(3000)
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
     * @param endTurn Ends current turn if true (default)
     * @param timeout Extra timeout duration per battle on top of user configuration. 0s default
     */
    protected suspend fun waitForTurnEnd(
        battles: Int,
        endTurn: Boolean = true,
        timeout: Long = 0
    ) {
        logger.info("Waiting for turn to end, expected battles: $battles")
        var battlesPassed = 0
        val wdt = WatchDogTimer(profile.combat.battleTimeout * 1000L + timeout)
        wdt.start()
        while (isActive && battlesPassed < battles) {
            if (isInBattle()) {
                wdt.reset()
                wdt.addTime((30 * gameState.delayCoefficient).roundToLong(), TimeUnit.SECONDS)
                clickThroughBattle()
                battlesPassed++
                wdt.reset()
            }
            if (wdt.hasExpired()) throw ScriptTimeOutException("Waiting for battles")
        }
        wdt.stop()
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
     * @param endTurn Ends turn automatically after waiting if true
     * @param timeout Time to wait before signalling timeout error and restart, 120s default
     */
    protected suspend fun waitForTurnAndPoints(
        turn: Int,
        points: Int,
        endTurn: Boolean = true,
        timeout: Long = 120_000
    ) = coroutineScope {
        logger.info("Waiting for turn $turn and action points $points")
        val ocr = Ocr.forConfig(config, digitsOnly = true)
        var currentTurn = 0
        var currentPoints = 0
        val wdt = WatchDogTimer(profile.combat.battleTimeout * 1000L + timeout)
        wdt.start()
        loop@ while (isActive && !interruptWaitFlag) {
            delay(500)
            when {
                isInBattle() -> {
                    wdt.reset()
                    wdt.addTime((30 * gameState.delayCoefficient).roundToLong(), TimeUnit.SECONDS)
                    clickThroughBattle()
                    wdt.reset()
                }
                currentTurn == turn && currentPoints == points -> break@loop
            }
            val screenshot = region.capture()
            val newTurn = ocr.doOCRAndTrim(screenshot.getSubimage(748, 53, 86, 72))
                .let { if (it.firstOrNull() == '8') it.replaceFirst("8", "0") else it }
                .toIntOrNull() ?: continue


            val newPoints = ocr.doOCRAndTrim(
                screenshot.getSubimage(1730, 970, 135, 76)
                    .binarizeImage().pad(10, 10, Color.BLACK)
            ).toIntOrNull() ?: continue

            if (newTurn > currentTurn || currentPoints != newPoints) {
                logger.info("Current turn: $newTurn ($turn) | Current action points: $newPoints ($points)")
                currentTurn = newTurn
                currentPoints = newPoints
                wdt.reset()
            }
            if (wdt.hasExpired()) throw ScriptTimeOutException("Waiting for turn and points")
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
     *
     * @param assets List of assets to wait for
     * @param endTurn Ends turn automatically after waiting if true
     * @param timeout Time to wait before signalling timeout error and restart, 120s default
     */
    protected suspend fun waitForTurnAssets(
        assets: List<ITemplate>,
        endTurn: Boolean = true,
        timeout: Long = 120_000
    ) {
        logger.info("Waiting for ${assets.size} assets to appear:")
        assets.forEach { logger.info("Waiting on: ${it.source}") }
        try {
            withTimeout(timeout) {
                while (isActive) {
                    if (isInBattle()) clickThroughBattle()
                    val r = region.asCachedRegion()
                    if (assets.all { r.has(it) }) break
                    yield()
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw ScriptTimeOutException("Waiting for assets", e)
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
        }
    }

    protected suspend fun terminateMission(incrementSorties: Boolean = true) {
        region.subRegion(370, 0, 220, 150)
            .waitHas(FileTemplate("combat/battle/terminate.png"), 5000)
        mapRunnerRegions.terminateMenu.click(); delay(1000)
        region.subRegion(1165, 650, 280, 170)
            .waitHas(FileTemplate("combat/battle/terminate-confirm.png"), 5000)
        mapRunnerRegions.terminate.click(); delay(5000)

        logger.info("Left battle screen")
        if (incrementSorties) scriptStats.sortiesDone += 1
        _battles = 1
    }

    protected suspend fun restartMission(incrementSorties: Boolean = true) {
        region.subRegion(370, 0, 220, 150)
            .waitHas(FileTemplate("combat/battle/terminate.png"), 5000)
        mapRunnerRegions.terminateMenu.click(); delay(1000)
        region.subRegion(715, 650, 280, 170)
            .waitHas(FileTemplate("combat/battle/restart-confirm.png"), 5000)
        mapRunnerRegions.restart.click(); delay(5000)

        logger.info("Restarted battle")
        if (incrementSorties) scriptStats.sortiesDone += 1
        _battles = 1
    }

    abstract suspend fun MapNode.findRegion(): AndroidRegion

    private suspend fun clickThroughBattle() {
        logger.info("Entered battle $_battles")
        onEnterBattleListener()
        // Delay to stop COOL Beach fairy BG change from triggering battle counter
        delay(4000)
        // Wait until it disappears
        while (isActive && isInBattle()) yield()
        logger.info("Battle ${_battles++} complete, clicking through battle results")
        // Animation and load wheel until you can click through results/drops
        delay(Random.nextLong(1100, 1300))
        val l = mapRunnerRegions.battleEndClick.randomPoint()
        val cancelR = region.subRegion(761, 674, 283, 144)
        var counter = 0
        val now = System.currentTimeMillis()
        loop@ while (true) {
            val capture = region.capture()
            val sample = Color(capture.getRGB(50, 1050))
            if (sample.isSimilar(Color(16, 16, 16)) &&
                Color(capture.getRGB(680, 580)).isSimilar(Color(222, 223, 74))
            ) {
                logger.info("Clicked until transition ($counter times)")
                break@loop
            }
            if (sample.isSimilar(Color(247, 0, 74))) {
                logger.info("Clicked until map ($counter times)")
                break@loop
            }
            if (config.scriptConfig.maxPostBattleClick != -1) {
                if (counter >= config.scriptConfig.maxPostBattleClick) {
                    logger.info("Clicked max times ($counter times)")
                    break@loop
                }
            }
            if (System.currentTimeMillis() - now > 10000) {
                logger.info("Clicked until timeout ($counter times)")
                break@loop
            }
            region.subRegion(l.x, l.y, 20, 20).click()
            cancelR.findBest(FileTemplate("combat/battle/cancel.png"))?.region?.click()
            ++counter
        }
        cancelR.findBest(FileTemplate("combat/battle/cancel.png"))?.region?.click()
        onFinishBattleListener()
    }

    private fun isInBattle() =
        mapRunnerRegions.pauseButton.has(FileTemplate("combat/battle/pause.png", 0.9))

    protected suspend fun openEchelon(node: MapNode) {
        val r = node.findRegion()
        val sr = region.subRegion(1430, 900, 640, 130)

        try {
            r.clickWhile(period = 2500, timeout = 30000) {
                sr.doesntHave(FileTemplate("cancel-deploy.png"))
            }
        } catch (e: TimeoutCancellationException) {
            throw ScriptTimeOutException("Opening echelon", e)
        }

        if (node.type == MapNode.Type.HeavyHeliport && gameState.requiresMapInit) {
            mapRunnerRegions.chooseEchelon.click(); delay(2000)
        }
    }

    private suspend fun endTurn() {
        mapRunnerRegions.endBattle.clickWhile { has(FileTemplate("combat/battle/end.png", 0.8)) }
        region.subRegion(1100, 675, 275, 130).waitHas(FileTemplate("ok.png"), 1000)?.click()
    }
}