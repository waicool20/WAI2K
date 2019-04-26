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

package com.waicool20.wai2k.script.modules

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.game.LogisticsSupport
import com.waicool20.wai2k.game.LogisticsSupport.Assignment
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.wai2k.util.formatted
import com.waicool20.waicoolutils.DurationUtils
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.mapAsync
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import kotlin.system.measureTimeMillis

class InitModule(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {
    private val logger = loggerFor<InitModule>()
    override suspend fun execute() {
        navigator.checkRequiresRestart()
        navigator.checkLogistics()
        if (gameState.requiresUpdate) updateGameState()
    }

    private suspend fun updateGameState() {
        navigator.navigateTo(LocationId.HOME_STATUS)
        logger.info("Updating gamestate")
        measureTimeMillis {
            val repairJob = launch { updateRepairs() }
            updateLogistics()
            // Wait for repairs to finish updating if script just started
            if (scriptStats.sortiesDone == 0) repairJob.join()
        }.let { logger.info("Finished updating game state in $it ms") }
        gameState.requiresUpdate = false
    }

    /**
     * Updates the logistic support in gamestate
     */
    private suspend fun updateLogistics() {
        logger.info("Reading logistics support status")
        // Optimize by taking a single screenshot and working on that
        val image = region.takeScreenshot()
        val entry = region.subRegion(485, 0, 240, region.h).findAllOrEmpty("init/logistics.png")
                // Map each region to whole logistic support entry
                .map { image.getSubimage(it.x - 135, it.y - 82, 853, 144) }
                .map {
                    listOf(
                            async {
                                // Echelon section on the right without the word "Echelon"
                                Ocr.forConfig(config, digitsOnly = true).doOCRAndTrim(it.getSubimage(0, 25, 83, 100))
                            },
                            async {
                                // Logistics number ie. 1-1
                                Ocr.forConfig(config).doOCRAndTrim(it.getSubimage(165, 30, 66, 33))
                            },
                            async {
                                // Timer xx:xx:xx
                                Ocr.forConfig(config).doOCRAndTrim(it.getSubimage(600, 71, 188, 40))
                            }
                    )
                }
                .map { "${it[0].await()} ${it[1].await()} ${it[2].await()}" }
                .mapNotNull {
                    Regex("(\\d) (\\d)\\s?[-—]\\s?(\\d) (\\d\\d):(\\d\\d):(\\d\\d)").matchEntire(it)?.destructured
                }
        // Clear existing timers
        gameState.echelons.forEach { it.logisticsSupportAssignment = null }
        entry.forEach { (sEchelon, sChapter, sNumber, sHour, sMinutes, sSeconds) ->
            val echelon = sEchelon.toInt()
            val logisticsSupport = LogisticsSupport.list[sChapter.toInt() * 4 + sNumber.toInt() - 1]
            val duration = DurationUtils.of(sSeconds.toLong(), sMinutes.toLong(), sHour.toLong())
            val eta = Instant.now() + duration
            logger.info("Echelon $echelon is doing logistics support ${logisticsSupport.formattedString}, ETA: ${eta.formatted()}")
            gameState.echelons[echelon - 1].logisticsSupportAssignment = Assignment(logisticsSupport, eta)
        }
    }

    /**
     * Updates the repair timers in gamestate
     */
    private suspend fun updateRepairs() {
        logger.info("Reading repair status")
        // Optimize by taking a single screenshot and working on that
        val image = region.takeScreenshot()

        val firstEntryRegion = Rectangle(450, 0, 159, region.h)
        val repairRegions = async {
            region.subRegion(firstEntryRegion).findAllOrEmpty("init/repairing.png")
        }
        val standbyRegions = async {
            region.subRegion(firstEntryRegion).findAllOrEmpty("init/standby.png")
        }
        val trainingRegions = async {
            region.subRegion(firstEntryRegion).findAllOrEmpty("init/in-training.png")
        }
        // Find all the echelons that have a girl in repair
        val entries = repairRegions.await() + standbyRegions.await() + trainingRegions.await()

        // Map each region to whole logistic support entry
        val mappedEntries = entries
                .map { image.getSubimage(it.x - 110, it.y - 11, 853, 144) }
                .map {
                    async {
                        // Echelon section on the right without the word "Echelon"
                        Ocr.forConfig(config, digitsOnly = true).doOCRAndTrim(it.getSubimage(0, 25, 83, 100))
                    } to async { readRepairTimers(it) }
                }.map { it.first.await().toInt() to it.second.await() }

        // Clear existing timers
        gameState.echelons.flatMap { it.members }.forEach { it.repairEta = null }
        mappedEntries.forEach { (echelon, repairTimers) ->
            logger.info("Echelon $echelon has repair timers: $repairTimers")
            repairTimers.forEach { (memberIndex, duration) ->
                gameState.echelons[echelon - 1].members[memberIndex].repairEta = Instant.now() + duration
            }
        }
    }

    /**
     * Reads repair timers for a single echelon row
     *
     * @param image Region containing a single echelon row
     * @returns Map with member index as key and repair timer duration as value
     */
    private suspend fun readRepairTimers(image: BufferedImage): Map<Int, Duration> {
        return (0 until 5).mapAsync(this) { entry ->
            // Single repair entry without the "Repairing" or "Standby"
            Ocr.forConfig(config).doOCRAndTrim(image.getSubimage(110 + 145 * entry, 45, 134, 65))
                    .takeIf { it.contains("Repairing") }
                    ?.let { timer ->
                        Regex("(\\d\\d):(\\d\\d):(\\d\\d)").find(timer)?.groupValues?.let {
                            entry to DurationUtils.of(it[3].toLong(), it[2].toLong(), it[1].toLong())
                        }
                    } ?: entry to Duration.ZERO
        }.toMap()
    }
}