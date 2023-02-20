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

package com.waicool20.wai2k.script.modules

import com.waicool20.cvauto.core.AnyRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.util.isSimilar
import com.waicool20.wai2k.game.LogisticsSupport
import com.waicool20.wai2k.game.LogisticsSupport.Assignment
import com.waicool20.wai2k.game.location.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.modules.combat.EmptyMapRunner
import com.waicool20.wai2k.util.*
import com.waicool20.waicoolutils.DurationUtils
import com.waicool20.waicoolutils.mapAsync
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.RasterFormatException
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.coroutineContext
import kotlin.system.measureTimeMillis

class InitModule(navigator: Navigator) : ScriptModule(navigator) {
    private val logger = loggerFor<InitModule>()
    override suspend fun execute() {
        navigator.checkRequiresRestart()
        navigator.checkLogistics()
        checkReset()

        if (!gameState.requiresUpdate) return
        updateGameState()
        if (!config.scriptConfig.idleAtHome && profile.logistics.enabled && !profile.combat.enabled) {
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

    private suspend fun checkReset() {
        if (Instant.now() > gameState.dailyReset) {
            logger.info("Server has reset!")
            navigator.navigateTo(LocationId.DAILY_LOGIN)
            delay(5000)
            gameState.dailyReset = gameState.nextReset()
            logger.info("Next server reset: ${gameState.dailyReset.formatted()}")
            navigator.navigateTo(LocationId.HOME)
        }
    }

    private suspend fun updateGameState() {
        navigator.navigateTo(LocationId.HOME_STATUS)
        if (region.subRegion(428, 0, 240, region.height / 2)
                .has(FileTemplate("init/in-combat.png"))
        ) {
            terminateExistingBattle()
            updateGameState()
            return
        }
        logger.info("Updating gamestate")
        val region = region.asCachedRegion()
        measureTimeMillis {
            val repairJob = scope.launch { updateRepairs(region) }
            updateLogistics(region)
            // Wait for repairs to finish updating if script just started
            if (scriptStats.sortiesDone == 0) repairJob.join()
        }.let { logger.info("Finished updating game state in $it ms") }
        gameState.requiresUpdate = false
    }

    /**
     * Updates the logistic support in gamestate
     */
    private suspend fun updateLogistics(cache: AnyRegion) {
        logger.info("Reading logistics support status")
        val entry = cache.subRegion(427, 0, 234, cache.height)
            .findBest(FileTemplate("init/logistics.png"), 4)
            .map { it.region }
            // Map each region to whole logistic support entry
            .map { cache.subRegion(it.x - 133, it.y - 87, 852, 115) }
            .mapAsync {
                val capture = it.capture()
                listOf(
                    // Echelon number
                    ocr.digitsOnly().readText(
                        capture.getSubimage(2, 25, 80, 90)
                    ),
                    // Logistics number ie. 1-1
                    ocr.useCharFilter(Ocr.DIGITS + "-").readText(
                        capture.getSubimage(165, 0, 90, 42), threshold = 0.2, invert = true
                    ),
                    // Timer xx:xx:xx
                    ocr.useCharFilter(Ocr.DIGITS + ":").readText(
                        capture.getSubimage(600, 70, 190, 42), threshold = 0.2, invert = true
                    )
                )
            }
            .map { "${it[0]} ${it[1]} ${it[2]}" }
            .mapNotNull {
                val m =
                    Regex("(\\d+) (\\d\\d?).*?(\\d) (\\d\\d):(\\d\\d):(\\d\\d)").matchEntire(it)?.destructured
                if (m == null) logger.debug("Detected something on status but failed OCR match: $it")
                m
            }
        // Clear existing timers
        gameState.echelons.forEach { it.logisticsSupportAssignment = null }
        entry.forEach { (sEchelon, sChapter, sNumber, sHour, sMinutes, sSeconds) ->
            val echelon = sEchelon.toInt()
            val logisticsSupport = LogisticsSupport.list[sChapter.toInt() * 4 + sNumber.toInt() - 1]
            val duration = DurationUtils.of(sSeconds.toLong(), sMinutes.toLong(), sHour.toLong())
            val eta = Instant.now() + duration
            logger.info("Echelon $echelon is doing logistics support ${logisticsSupport.formattedString}, ETA: ${eta.formatted()}")
            gameState.echelons[echelon - 1].logisticsSupportAssignment =
                Assignment(logisticsSupport, eta)
        }
    }

    /**
     * Updates the repair timers in gamestate
     */
    private suspend fun updateRepairs(cache: AnyRegion) {
        logger.info("Reading repair status")

        val firstEntryRegion = cache.subRegion(388, 0, 159, region.height)
        val repairRegions = scope.async {
            firstEntryRegion.findBest(FileTemplate("init/repairing.png"), 6)
        }
        val standbyRegions = scope.async {
            firstEntryRegion.findBest(FileTemplate("init/standby.png"), 6)
        }
        val trainingRegions = scope.async {
            firstEntryRegion.findBest(FileTemplate("init/in-training.png"), 6)
        }
        // Find all the echelons that have a girl in repair
        val entries = repairRegions.await() + standbyRegions.await() + trainingRegions.await()

        // Map each region to whole logistic support entry
        val mappedEntries = entries.map { it.region }
            .mapNotNull {
                try {
                    cache.capture().getSubimage(it.x - 109, it.y - 31, 852, 184)
                } catch (e: RasterFormatException) {
                    logger.warn("One status entry is out of bounds, ignoring!")
                    null
                }
            }
            .map {
                scope.async {
                    // Echelon number
                    ocr.digitsOnly().readText(it.getSubimage(0, 25, 80, 125))
                } to scope.async { readRepairTimers(it) }
            }.map { it.first.await().toInt() to it.second.await() }

        // Clear existing timers
        gameState.echelons.flatMap { it.members }.forEach { it.repairEta = null }
        mappedEntries.forEach { (echelon, repairTimers) ->
            logger.info("Echelon $echelon has repair timers: $repairTimers")
            repairTimers.forEach { (memberIndex, duration) ->
                gameState.echelons[echelon - 1].members[memberIndex].repairEta =
                    Instant.now() + duration
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
            ocr.readText(image.getSubimage(115 + 145 * entry, 66, 122, 75), invert = true)
                .takeIf { it.contains("Repairing") }
                ?.let { timer ->
                    Regex("(\\d\\d):(\\d\\d):(\\d\\d)").find(timer)?.groupValues?.let {
                        entry to DurationUtils.of(it[3].toLong(), it[2].toLong(), it[1].toLong())
                    }
                } ?: (entry to Duration.ZERO)
        }.toMap()
    }

    private suspend fun terminateExistingBattle() {
        logger.info("Detected ongoing battle, terminating it first")
        while (coroutineContext.isActive) {
            val capture = region.capture()
            if (Color(capture.getRGB(50, 1050)).isSimilar(Color(16, 16, 16)) &&
                Color(capture.getRGB(680, 580)).isSimilar(Color(222, 223, 74))
            ) break
            // Region that contains the word `RESUME` if there's an ongoing battle
            region.subRegion(1800, 780, 170, 75).click()
            navigator.checkLogistics()
        }
        logger.info("Transitioning to map")
        while (coroutineContext.isActive) {
            if (region.has(FileTemplate("combat/battle/terminate.png"))) break
            delay(500)
        }
        logger.info("Entered map")

        val terminateMapRunner = object : EmptyMapRunner(this@InitModule) {
            override suspend fun begin() {
                delay(5000)
                waitForTurnAssets(listOf(FileTemplate("combat/battle/terminate.png")), false)
                terminateMission(false)
            }
        }

        terminateMapRunner.begin()
        delay(5000)
    }
}
