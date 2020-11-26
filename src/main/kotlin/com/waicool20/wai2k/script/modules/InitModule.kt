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

import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.asCachedRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.game.LogisticsSupport
import com.waicool20.wai2k.game.LogisticsSupport.Assignment
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.wai2k.util.formatted
import com.waicool20.waicoolutils.DurationUtils
import com.waicool20.waicoolutils.logging.loggerFor
import com.waicool20.waicoolutils.mapAsync
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.Instant
import kotlin.system.measureTimeMillis

class InitModule(navigator: Navigator) : ScriptModule(navigator) {
    private val logger = loggerFor<InitModule>()
    override suspend fun execute() {
        navigator.checkRequiresRestart()
        navigator.checkLogistics()
        if (gameState.requiresUpdate) {
            updateGameState()
            if (profile.logistics.enabled && !profile.combat.enabled) {
                // Workaround for GFL game freeze at home screen if there are dolls training,
                // remove when MICA finally fixes this
                listOf(LocationId.FORMATION, LocationId.COMBAT_MENU)
                    .random()
                    .let {
                        logger.info("Idling @ $it")
                        navigator.navigateTo(it)
                    }
            }
        }
    }

    private suspend fun updateGameState() {
        navigator.navigateTo(LocationId.HOME_STATUS)
        logger.info("Updating gamestate")
        val region = region.asCachedRegion()
        measureTimeMillis {
            val repairJob = launch { updateRepairs(region) }
            updateLogistics(region)
            // Wait for repairs to finish updating if script just started
            if (scriptStats.sortiesDone == 0) repairJob.join()
        }.let { logger.info("Finished updating game state in $it ms") }
        gameState.requiresUpdate = false
    }

    /**
     * Updates the logistic support in gamestate
     */
    private suspend fun updateLogistics(cache: Region<AndroidDevice>) {
        logger.info("Reading logistics support status")
        val entry = cache.subRegion(422, 0, 240, cache.height)
            .findBest(FileTemplate("init/logistics.png"), 4)
            .map { it.region }
            // Map each region to whole logistic support entry
            .map { cache.subRegion(it.x - 133, it.y - 87, 852, 115) }
            .mapAsync {
                listOf(
                    // Echelon number
                    Ocr.forConfig(config, digitsOnly = true).doOCRAndTrim(it.subRegion(0, 25, 80, 90)),
                    // Logistics number ie. 1-1
                    Ocr.forConfig(config).doOCRAndTrim(it.subRegion(165, 0, 90, 42)),
                    // Timer xx:xx:xx
                    Ocr.forConfig(config).doOCRAndTrim(it.subRegion(600, 70, 190, 42))
                )
            }
            .map { "${it[0]} ${it[1]} ${it[2]}" }
            .mapNotNull {
                Regex("(\\d+) (\\d\\d?).*?(\\d) (\\d\\d):(\\d\\d):(\\d\\d)").matchEntire(it)?.destructured
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
    private suspend fun updateRepairs(cache: Region<AndroidDevice>) {
        logger.info("Reading repair status")

        val firstEntryRegion = cache.subRegion(388, 0, 159, region.height)
        val repairRegions = async {
            firstEntryRegion.findBest(FileTemplate("init/repairing.png"), 6)
        }
        val standbyRegions = async {
            firstEntryRegion.findBest(FileTemplate("init/standby.png"), 6)
        }
        val trainingRegions = async {
            firstEntryRegion.findBest(FileTemplate("init/in-training.png"), 6)
        }
        // Find all the echelons that have a girl in repair
        val entries = repairRegions.await() + standbyRegions.await() + trainingRegions.await()

        // Map each region to whole logistic support entry
        val mappedEntries = entries.map { it.region }
            .map { cache.capture().getSubimage(it.x - 109, it.y - 31, 852, 184) }
            .map {
                async {
                    // Echelon number
                    Ocr.forConfig(config, digitsOnly = true).doOCRAndTrim(it.getSubimage(0, 25, 80, 125))
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
        return (0 until 5).mapAsync { entry ->
            // Single repair entry without the "Repairing" or "Standby"
            Ocr.forConfig(config).doOCRAndTrim(image.getSubimage(115 + 145 * entry, 66, 122, 75))
                .takeIf { it.contains("Repairing") }
                ?.let { timer ->
                    Regex("(\\d\\d):(\\d\\d):(\\d\\d)").find(timer)?.groupValues?.let {
                        entry to DurationUtils.of(it[3].toLong(), it[2].toLong(), it[1].toLong())
                    }
                } ?: entry to Duration.ZERO
        }.toMap()
    }
}