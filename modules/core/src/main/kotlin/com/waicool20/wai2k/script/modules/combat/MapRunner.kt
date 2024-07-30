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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FT
import com.waicool20.cvauto.core.template.ITemplate
import com.waicool20.cvauto.core.util.countColor
import com.waicool20.cvauto.core.util.isSimilar
import com.waicool20.cvauto.core.util.pipeline
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.events.RepairEvent
import com.waicool20.wai2k.events.SortieDoneEvent
import com.waicool20.wai2k.game.CombatMap
import com.waicool20.wai2k.game.Echelon
import com.waicool20.wai2k.game.GFL
import com.waicool20.wai2k.game.MapRunnerRegions
import com.waicool20.wai2k.script.ScriptComponent
import com.waicool20.wai2k.script.ScriptTimeOutException
import com.waicool20.wai2k.util.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.reflections.Reflections
import java.awt.Color
import java.awt.image.BufferedImage
import java.lang.reflect.Modifier
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

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
) : ScriptComponent by scriptComponent {

    class Deployment(val echelon: Int, val mapNode: MapNode) : Deployable
    class Retreat(val mapNode: MapNode) : Retreatable
    data class TurnInfo(val turn: Int, val points: Int)

    @Target(AnnotationTarget.CLASS)
    annotation class DisableMap(val reason: String = "")

    infix fun Int.at(mapNode: MapNode) = Deployment(this, mapNode)

    companion object {
        private val logger = loggerFor<Companion>()
        val list = mutableMapOf<CombatMap, KClass<out MapRunner>>()

        init {
            val mapClasses = Reflections("com.waicool20.wai2k.script.modules.combat.maps")
                .getSubTypesOf(MapRunner::class.java)
            for (mapClass in mapClasses) {
                if (Modifier.isAbstract(mapClass.modifiers)) continue
                if (Modifier.isInterface(mapClass.modifiers)) continue
                val an = mapClass.kotlin.findAnnotation<DisableMap>()
                if (an != null) {
                    logger.warn("Map ${mapClass.simpleName} disabled, reason: ${an.reason}")
                    continue
                }
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
    protected suspend fun deployEchelons(vararg deployments: Deployable): Set<MapNode> =
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
                if (echelon in 1..GFL.MAX_ECHELON) {
                    while (!Echelon(echelon).clickEchelon(this@MapRunner, 140)) delay(200)
                }
                val screenshot = region.capture().img
                val formatter = DecimalFormat("##.#")

                val members = if (this@MapRunner is CorpseDragging) {
                    logger.info("Corpse dragging map, only member ${profile.combat.draggerSlot} will be scanned")
                    listOf(profile.combat.draggerSlot - 1)
                } else {
                    (0..4).toList()
                }.filter { hasMember(screenshot, it) }

                // MICA why are your things 1 pixel off :(
                val xOffsets = arrayOf(253, 525, 798, 1071, 1344)
                val wOffsets = arrayOf(217, 218, 218, 218, 217)
                val ammoNeedsSupply = members.associateWith { m ->
                    val image = screenshot.getSubimage(
                        xOffsets[m], 820, wOffsets[m], 1
                    ).pipeline().threshold().toBufferedImage()
                    val ammoCount = image.countColor(Color.WHITE) / image.width.toDouble()
                    if (ammoCount < ammoResupplyThreshold) needsResupply += node
                    ammoCount
                }
                val rationNeedsSupply = members.associateWith { m ->
                    val image = screenshot.getSubimage(
                        xOffsets[m], 860, wOffsets[m], 1
                    ).pipeline().threshold().toBufferedImage()
                    val rationCount = image.countColor(Color.WHITE) / image.width.toDouble()
                    if (rationCount < rationsResupplyThreshold) needsResupply += node
                    rationCount
                }

                val hpMap = members.associateWith { m ->
                    val image = screenshot.getSubimage(
                        xOffsets[m], 778, wOffsets[m], 1
                    ).pipeline().threshold().toBufferedImage()
                    image.countColor(Color.WHITE) / image.width.toDouble() * 100
                }

                logger.info("----- Members -----")
                members.forEach { m ->
                    val ammo = formatter.format(ammoNeedsSupply[m]!! * 100)
                    val rations = formatter.format(rationNeedsSupply[m]!! * 100)
                    val hp = hpMap[m]?.takeIf { it in 0.0..100.0 }
                        ?.let { formatter.format(it) } ?: "N/A"
                    logger.info("Member ${m + 1} | HP: $hp%\t\t| Ammo: $ammo%\t\t| Rations: $rations%")
                }
                logger.info("-------------------")

                if (this@MapRunner !is CorpseDragging) {
                    members.forEach { m ->
                        if (hpMap[m]!! in 0.0..profile.combat.repairThreshold.toDouble()) {
                            logger.info("Repairing member ${m + 1}")
                            region.subRegion(239 + m * 272, 228, 246, 323).click()
                            region.subRegion(1441, 772, 250, 96)
                                .waitHas(FT("ok.png"), 3000)?.click()
                            EventBus.publish(
                                RepairEvent(
                                    1,
                                    profile.combat.map,
                                    sessionId,
                                    elapsedTime
                                )
                            )
                            delay(500)
                        }
                    }
                }

                mapRunnerRegions.deploy.click()
                delay(2000)
            }
            needsResupply.forEach { logger.info("Echelon at $it needs resupply!") }
            delay(200)
            logger.info("Deployment complete")
            needsResupply
        }

    /**
     * Resupplies an echelon at the given nodes, skips normal type nodes
     *
     * @param node Nodes to resupply
     */
    protected suspend fun resupplyEchelons(vararg node: MapNode) = resupplyEchelons(node.toSet())

    /**
     * Resupplies an echelon at the given nodes, skips normal type nodes
     *
     * @param nodes Nodes to resupply
     */
    protected suspend fun resupplyEchelons(nodes: Set<MapNode>) {
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

    /**
     * Retreats an echelon at the given nodes, skips normal type nodes
     *
     * @param retreat Nodes to retreat
     */
    protected suspend fun retreatEchelons(vararg retreat: Retreatable) =
        retreatEchelons(retreat.toSet())

    /**
     * Retreats an echelon at the given nodes, skips normal type nodes
     *
     * @param retreats Nodes to retreat
     */
    protected suspend fun retreatEchelons(retreats: Set<Retreatable>) {
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
        region.subRegion(995, 696, 250, 95).click()
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
        region.waitHas(FT("combat/battle/switch.png", 0.95), 2000)?.click()
        delay(3000)
    }

    /**
     * Waits for the G&K splash animation that appears at the beginning of the turn to appear
     * and waits for it to disappear
     *
     * @param timeout Max amount of time to wait for splash, can be set to longer lengths for
     * between turns
     */
    protected suspend fun waitForGNKSplash(timeout: Long = 10000) {
        logger.info("Waiting for G&K splash screen")
        val start = System.currentTimeMillis()
        // Wait for the G&K splash to appear within 10 seconds
        while (coroutineContext.isActive) {
            delay(500)
            if (ocr.readText(mapRunnerRegions.endBattle, threshold = 0.73).contains("end", true)) {
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
        while (coroutineContext.isActive && battlesPassed < battles) {
            if (isInBattle()) {
                wdt.reset()
                wdt.addTime((30 * gameState.delayCoefficient).roundToLong(), TimeUnit.SECONDS)
                clickThroughBattle()
                battlesPassed++
                wdt.reset()
            }
            delay(500)
            if (wdt.hasExpired()) throw ScriptTimeOutException("Waiting for battles")
        }
        wdt.stop()
        region.waitHas(FT("combat/battle/terminate.png"), 10000)
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
    ) {
        logger.info("Waiting for turn $turn and action points $points")
        var oldTurn = 0
        var oldPoints = 0
        val wdt = WatchDogTimer(profile.combat.battleTimeout * 1000L + timeout)
        loop@ while (coroutineContext.isActive && !interruptWaitFlag) {
            delay(500)
            when {
                isInBattle() -> {
                    wdt.reset()
                    wdt.addTime((30 * gameState.delayCoefficient).roundToLong(), TimeUnit.SECONDS)
                    clickThroughBattle()
                    wdt.reset()
                }
                oldTurn == turn && oldPoints == points -> break@loop
            }

            val (newTurn, newPoints) = getTurnInfo() ?: continue

            if (newTurn > oldTurn || oldPoints != newPoints) {
                logger.info("Current turn: $newTurn ($turn) | Current action points: $newPoints ($points)")
                oldTurn = newTurn
                oldPoints = newPoints
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
        while (coroutineContext.isActive) {
            if (isInBattle()) clickThroughBattle()
            if (region.has(FT("combat/battle/terminate.png"))) {
                delay(5000)
                if (region.has(FT("combat/battle/terminate.png"))) break
            }
            delay(500)
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
        val wdt = WatchDogTimer(profile.combat.battleTimeout * 1000L + timeout)
        val popup = region.subRegion(915, 730, 490, 60)
        while (coroutineContext.isActive) {
            if (ocr.readText(popup, scale = 0.8, threshold = 0.7).contains("start", true)) {
                logger.info("Enemy encounter!")
                delay(1000)
                popup.click()
            }
            if (isInBattle()) {
                wdt.reset()
                wdt.addTime((30 * gameState.delayCoefficient).roundToLong(), TimeUnit.SECONDS)
                clickThroughBattle()
                wdt.reset()
            }
            val r = region.freeze()
            if (assets.all { r.has(it) }) break
            if (wdt.hasExpired()) throw ScriptTimeOutException("Waiting for turn assets")
            delay(500)
        }
        logger.info("All assets are now on screen")
        region.waitHas(FT("combat/battle/terminate.png"), 10000)
        if (endTurn) endTurn()
    }

    /**
     * Clicks through the battle results and waits for the game to return to the combat menu
     */
    protected suspend fun handleBattleResults() {
        logger.info("Battle ended, clicking through battle results")
        if (this@MapRunner is EventMapRunner) {
            logger.info("Waiting for event menu")
        } else {
            logger.info("Waiting for combat menu")
        }
        waitForLog("UICombatSettlement(Clone)")
        waitForLog("MissionSelectionController:Start()") {
            repeat(Random.nextInt(2, 4)) {
                mapRunnerRegions.battleEndClick.click()
                delay(50)
            }
            delay(500)
        }
        delay(5000)
        EventBus.publish(
            SortieDoneEvent(
                profile.combat.map,
                if (this is CorpseDragging) profile.combat.draggers else emptyList(),
                sessionId,
                elapsedTime
            )
        )
    }

    protected suspend fun terminateMission(incrementSorties: Boolean = true) {
        mapRunnerRegions.terminateMenu.clickWhile(period = 3000) {
            region.subRegion(1044, 650, 280, 170)
                .doesntHave(FT("combat/battle/terminate-confirm.png"))
        }
        region.subRegion(1044, 650, 280, 170)
            .waitHas(FT("combat/battle/terminate-confirm.png"), 10000)
        mapRunnerRegions.terminate.click(); delay(5000)

        logger.info("Left battle screen")
        if (incrementSorties) EventBus.publish(
            SortieDoneEvent(
                profile.combat.map,
                if (this is CorpseDragging) profile.combat.draggers else emptyList(),
                sessionId,
                elapsedTime
            )
        )
        _battles = 1
    }

    protected suspend fun restartMission(incrementSorties: Boolean = true) {
        mapRunnerRegions.terminateMenu.clickWhile(period = 3000) {
            region.subRegion(598, 650, 280, 170)
                .doesntHave(FT("combat/battle/restart-confirm.png"))
        }
        region.subRegion(598, 650, 280, 170)
            .waitHas(FT("combat/battle/restart-confirm.png"), 10000)
        mapRunnerRegions.restart.click(); delay(5000)

        logger.info("Restarted battle")
        if (incrementSorties) EventBus.publish(
            SortieDoneEvent(
                profile.combat.map,
                if (this is CorpseDragging) profile.combat.draggers else emptyList(),
                sessionId,
                elapsedTime
            )
        )
        _battles = 1
    }

    protected suspend fun combatSettlement(incrementSorties: Boolean = true) {
        logger.info("Combat settlement")
        mapRunnerRegions.terminateMenu.click(); delay(1000)
        // Click ok
        region.subRegion(1115, 696, 250, 96).click()
        if (incrementSorties) EventBus.publish(
            SortieDoneEvent(
                profile.combat.map,
                if (this is CorpseDragging) profile.combat.draggers else emptyList(),
                sessionId,
                elapsedTime
            )
        )
        _battles = 1
    }

    abstract suspend fun MapNode.findRegion(): AndroidRegion

    private suspend fun clickThroughBattle() {
        logger.info("Entered battle $_battles")
        onEnterBattleListener()
        // Delay to stop COOL Beach fairy BG change from triggering battle counter
        delay(4000)
        // Wait until it disappears
        while (coroutineContext.isActive && isInBattle()) delay(500)
        logger.info("Battle ${_battles++} complete, clicking through battle results")
        // Animation and load wheel until you can click through results/drops
        delay(Random.nextLong(1100, 1300))
        val l = mapRunnerRegions.battleEndClick.randomPoint()
        var counter = 0
        val now = System.currentTimeMillis()
        loop@ while (true) {
            val cache = region.freeze()
            val sample = cache.pickColor(30, 1037)
            if (sample.isSimilar(Color(18, 18, 15)) &&
                cache.pickColor(680, 480).isSimilar(Color(221, 220, 72))
            ) {
                logger.info("Clicked until transition ($counter times)")
                break@loop
            }
            if (sample.isSimilar(Color(195, 44, 88))) {
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
            region.findBest(FT("combat/battle/cancel.png"))?.region?.click()
            ++counter
        }
        region.findBest(FT("combat/battle/cancel.png"))?.region?.click()
        onFinishBattleListener()
    }

    private fun isInBattle() =
        mapRunnerRegions.pauseButton.has(FT("combat/battle/pause.png", 0.9))

    protected suspend fun openEchelon(node: MapNode) {
        val r = node.findRegion()
        val sr = region.subRegion(1234, 900, 640, 130)

        try {
            r.clickWhile(period = 2500, timeout = 30000) {
                sr.doesntHave(FT("cancel-deploy.png"))
            }
        } catch (e: TimeoutCancellationException) {
            throw ScriptTimeOutException("Opening echelon", e)
        }

        if (node.type == MapNode.Type.HeavyHeliport && gameState.requiresMapInit) {
            mapRunnerRegions.chooseEchelon.click(); delay(2000)
        }
        logger.info("Waiting for echelons to show up")
        while (coroutineContext.isActive) {
            if (hasMember(region.capture().img, 0)) break;
        }
    }

    private suspend fun endTurn() {
        waitForLog("Dequeue:Mission/endTurn") {
            mapRunnerRegions.endBattle.click()
            delay(200)
        }
    }

    /**
     * Reads the current turn and point count
     */
    protected fun getTurnInfo(): TurnInfo? {
        val ocr = ocr.useCharFilter(Ocr.DIGITS + "-")
        val screenshot = region.capture().img

        val turn = ocr.readText(screenshot.getSubimage(748, 53, 86, 72), invert = true)
            .let { if (it.firstOrNull() == '8') it.replaceFirst("8", "0") else it }
            .toIntOrNull() ?: return null

        val points = ocr.readText(
            screenshot.getSubimage(1730, 970, 140, 76),
            threshold = 0.2, invert = true
        ).toIntOrNull() ?: return null

        return TurnInfo(turn, points)
    }

    /**
     * Reads the current turn count and returns true if equal to [targetPoints]
     */
    protected fun checkPoints(targetPoints: Int): Boolean {
        val wdt = WatchDogTimer(20_000)
        while (true) {
            if (wdt.hasExpired()) throw ScriptTimeOutException("Checking points")
            val (_, points) = getTurnInfo() ?: continue
            wdt.stop()
            if (points != targetPoints) {
                logger.info("Remaining AP should be $targetPoints, got $points instead")
                return false
            }
            return true
        }
    }

    /**
     * Enter planning mode if it isn't already
     */
    protected suspend fun enterPlanningMode() {
        while (coroutineContext.isActive) {
            if (region.pickColor(125, mapRunnerRegions.planningMode.y).isSimilar(Color.WHITE)) {
                logger.info("Entering planning mode")
                mapRunnerRegions.planningMode.click()
                delay((3000 * gameState.delayCoefficient).roundToLong())
            } else {
                logger.info("In planning mode")
                break
            }
            delay(500)
        }
    }

    /**
     * Select nodes
     */
    protected suspend fun selectNodes(vararg indices: Int) {
        for (i in indices) {
            logger.info("Selecting node ${nodes[i]}")
            nodes[i].findRegion().click()
            delay(200)
        }
    }

    private fun hasMember(screenshot: BufferedImage, mIndex: Int): Boolean {
        return Color(screenshot.getRGB(266 + mIndex * 272, 765)).isSimilar(Color.WHITE)
    }
}
