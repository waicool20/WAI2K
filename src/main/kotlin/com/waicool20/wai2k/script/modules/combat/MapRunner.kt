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
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import org.reflections.Reflections
import kotlin.coroutines.CoroutineContext

abstract class MapRunner(
        protected val scriptRunner: ScriptRunner,
        protected val region: AndroidRegion,
        protected val config: Wai2KConfig,
        protected val profile: Wai2KProfile
) : CoroutineScope {
    private val logger = loggerFor<MapRunner>()
    private var _currentNode = 1

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
     * Returns the current node number where the main echelon is at, or no. of battles that
     * have passed
     */
    val currentNode get() = _currentNode

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
     * @param echelons A variable list of pairs that contain the destination label and
     * the corresponding click region (Heliport, Command post etc.)
     */
    protected suspend fun deployEchelons(vararg echelons: Pair<String, AndroidRegion>) {
        echelons.forEachIndexed { i, (label, region) ->
            logger.info("Deploying echelon ${i + 1} to $label")
            region.clickRandomly(); delay(300)
            logger.info("Pressing the ok button")
            mapRunnerRegions.deploy.clickRandomly()
            delay(300)
        }

        logger.info("Deployment complete")
    }

    /**
     * Resupplies an echelon at the given location using click regions
     *
     * @param label Destination label, used for logging
     * @param region Click region of the destination (Heliport, Command post etc.)
     */
    protected suspend fun resupplyEchelon(label: String, region: AndroidRegion) {
        logger.info("Resupplying echelon at $label")
        // Clicking twice, first to highlight the echelon, the second time to enter the deployment menu
        logger.info("Selecting echelon")
        region.apply {
            clickRandomly(); yield()
            clickRandomly(); delay(300)
        }
        logger.info("Resupplying")
        mapRunnerRegions.resupply.clickRandomly()
        // Close dialog in case echelon doesn't need resupply
        region.findOrNull("close.png")?.clickRandomly()
        logger.info("Resupply complete")
    }

    /**
     * Waits for the G&K splash animation that appears at the beginning of the turn to appear
     * and waits for it to disappear
     */
    protected suspend fun waitForGNKSplash() {
        logger.info("Waiting for G&K splash screen")
        // Wait for the G&K splash to appear within 10 seconds
        region.waitSuspending("combat/battle/splash.png", 10).apply {
            logger.info("G&K splash screen appeared")
            delay(2000)
        } ?: logger.info("G&K splash screen did not appear")
    }

    /**
     * Waits for the current turn to end by counting the amount of battles that have passed
     * then ends the turn. This also clicks through any battle results when node battle ends
     *
     * @param battles Amount of battles expected in this turn
     */
    protected suspend fun waitForTurnEnd(battles: Int) {
        logger.info("Waiting for battle to end")
        var passedNodes = 0
        val clickRegion = region.subRegion(1960, 90, 200, 200)
        while (passedNodes < battles && isActive) {
            if (clickRegion.has("combat/battle/autoskill.png", 0.75)) {
                logger.info("Entered node $_currentNode")
                // Wait until it disappears
                while (clickRegion.has("combat/battle/autoskill.png", 0.75)) yield()
                logger.info("Node ${_currentNode++} battle complete, clicking through battle results")
                val l = clickRegion.randomLocation()
                repeat(6) { region.click(l); yield() }
                passedNodes++
            }
        }
        region.waitSuspending("combat/battle/terminate.png", 1200)
        logger.info("Turn ended")
        // Click end button
        mapRunnerRegions.endBattle.clickRandomly()
    }

    /**
     * Clicks through the battle results and waits for the game to return to the combat menu
     */
    protected suspend fun handleBattleResults() {
        logger.info("Battle ended, clicking through battle results")
        val combatMenu = GameLocation.mappings(config)[LocationId.COMBAT_MENU]!!
        val clickLocation = region.subRegion(992, 24, 1168, 121).randomLocation()
        val clickJob = launch {
            while (isActive) region.click(clickLocation)
        }
        while (isActive) {
            if (combatMenu.isInRegion(region)) break
        }
        clickJob.cancel()
        logger.info("Back at combat menu")
        scriptStats.sortiesDone += 1
        _currentNode = 1
    }
}