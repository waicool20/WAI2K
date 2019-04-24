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
import com.waicool20.wai2k.game.MapRunnerRegions
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.binarizeImage
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.pad
import kotlinx.coroutines.*
import org.reflections.Reflections
import java.awt.Color
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.random.Random

abstract class MapRunner(
        protected val scriptRunner: ScriptRunner,
        protected val region: AndroidRegion,
        protected val config: Wai2KConfig,
        protected val profile: Wai2KProfile
) : CoroutineScope {
    protected data class Deployment(val label: String, val region: AndroidRegion)

    private val logger = loggerFor<MapRunner>()
    private val pauseButtonRegion = region.subRegion(1020, 0, 110, 50)
    private val battleEndClickRegion = region.subRegion(992, 24, 1168, 121)
    private var _battles = 1

    companion object {
        const val COMMAND_POST = "command post"
        const val HELIPORT = "heliport"

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
     * @param deployments A variable list of [Deployment] that contains the destination label and
     * the corresponding click region (Heliport, Command post etc.)
     */
    protected suspend fun deployEchelons(vararg deployments: Deployment) {
        deployments.forEachIndexed { i, (label, region) ->
            logger.info("Deploying echelon ${i + 1} to $label")
            region.clickRandomly(); delay(300)
            mapRunnerRegions.deploy.clickRandomly()
            delay(300)
        }

        logger.info("Deployment complete")
    }

    /**
     * Resupplies an echelon at the given location using click regions
     *
     * @param deployment [Deployment] that contains the destination label and
     * the corresponding click region (Heliport, Command post etc.)
     */
    protected suspend fun resupplyEchelon(deployment: Deployment) {
        logger.info("Resupplying echelon at ${deployment.label}")
        // Clicking twice, first to highlight the echelon, the second time to enter the deployment menu
        logger.info("Selecting echelon")
        deployment.region.apply {
            clickRandomly(); yield()
            clickRandomly(); delay(300)
        }
        logger.info("Resupplying")
        mapRunnerRegions.resupply.clickRandomly()
        logger.info("Resupply complete")
        delay(400)
    }

    /**
     * Waits for the G&K splash animation that appears at the beginning of the turn to appear
     * and waits for it to disappear
     *
     * @param timeout Max amount of time to wait for splash, can be set to longer lengths for
     * between turns
     */
    protected suspend fun waitForGNKSplash(timeout: Long = 10) {
        logger.info("Waiting for G&K splash screen")
        val battleClicker = launch {
            while (isActive) {
                if (isInBattle()) {
                    logger.info("Entered enemy battle $_battles")
                    // Wait until it disappears
                    while (isActive && isInBattle()) yield()
                    logger.info("Battle ${_battles++} complete, clicking through battle results")
                    delay(400)
                    val l = battleEndClickRegion.randomLocation()
                    repeat(Random.nextInt(7, 9)) { region.click(l); yield() }
                } else yield()
            }
        }
        // Wait for the G&K splash to appear within 10 seconds
        region.waitSuspending("combat/battle/splash.png", timeout)?.apply {
            logger.info("G&K splash screen appeared")
            delay(2000)
        } ?: logger.info("G&K splash screen did not appear")
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
        region.waitSuspending("combat/battle/terminate.png", 1200)
        logger.info("Turn ended")
        // Click end button
        delay(400)
        repeat(Random.nextInt(2, 3)) { mapRunnerRegions.endBattle.clickRandomly() }
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
            val screenshot = region.takeScreenshot()
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
            if (region.has("combat/battle/terminate.png")) break
        }
        repeat(Random.nextInt(2, 3)) { mapRunnerRegions.endBattle.clickRandomly() }
    }

    private suspend fun clickThroughBattle() {
        logger.info("Entered battle $_battles")
        // Wait until it disappears
        while (isActive && isInBattle()) yield()
        logger.info("Battle ${_battles++} complete, clicking through battle results")
        delay(400)
        val l = battleEndClickRegion.randomLocation()
        repeat(Random.nextInt(7, 9)) { region.click(l); yield() }
    }

    /**
     * Clicks through the battle results and waits for the game to return to the combat menu
     */
    protected suspend fun handleBattleResults() {
        logger.info("Battle ended, clicking through battle results")
        val combatMenu = GameLocation.mappings(config)[LocationId.COMBAT_MENU]!!
        val clickLocation = battleEndClickRegion.randomLocation()
        val clickJob = launch {
            while (isActive) region.click(clickLocation)
        }
        while (isActive) {
            if (combatMenu.isInRegion(region)) break
        }
        clickJob.cancel()
        logger.info("Back at combat menu")
        scriptStats.sortiesDone += 1
        _battles = 1
    }

    protected infix fun String.at(region: AndroidRegion) = Deployment(this, region)

    private fun isInBattle() = pauseButtonRegion.has("combat/battle/pause.png", 0.9)
}