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
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import org.sikuli.script.Image
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class FactoryModule(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile,
        navigator: Navigator
) : ScriptModule(scriptRunner, region, config, profile, navigator) {
    private val logger = loggerFor<FactoryModule>()

    override suspend fun execute() {
        checkDollOverflow()
    }

    private suspend fun checkDollOverflow() {
        if (!gameState.dollOverflow) return
        if (profile.factory.enhancement.enabled) enhanceDolls()
        if (!gameState.dollOverflow) return
        if (profile.factory.disassembly.enabled) disassembleDolls()
    }

    /**
     * Keeps enhancing dolls until there are no more 2 star dolls
     */
    private suspend fun enhanceDolls() {
        logger.info("Doll limit reached, will try to enhance")
        navigator.navigateTo(LocationId.TDOLL_ENHANCEMENT)

        var oldDollCount: List<String>? = null
        val dollsUsedForEnhancement = AtomicInteger(0)
        val statUpdateJobs = mutableListOf<Job>()

        while (isActive) {
            val selectCharacterButton = region.subRegion(464, 189, 264, 497)
            // Click select character
            selectCharacterButton.clickRandomly(); delay(500)

            // Find the old doll count
            statUpdateJobs += updateJob(region.subRegion(1750, 810, 290, 70).takeScreenshot()) { count ->
                val c = count[0].toInt()
                oldDollCount?.get(0)?.toIntOrNull()?.let {
                    dollsUsedForEnhancement.getAndAdd(it - c)
                }
                oldDollCount = count
                c >= count[1].toInt()
            }

            logger.info("Selecting highest level T-doll for enhancement")
            // Randomly select a doll on the screen for enhancement
            while (isActive) {
                val doll = region.findAllOrEmpty("doll-list/lock.png")
                        .also { logger.info("Found ${it.size} dolls on screen available for enhancement") }
                        // Map lock region to doll region
                        .map { region.subRegion(it.x - 7, it.y, 244, it.h) }
                        // Prioritize higher level dolls
                        .sortedBy { it.y * 10 + it.x }
                        .firstOrNull()
                if (doll == null) {
                    if (region.findAllOrEmpty("doll-list/logistics.png").size >= 12) {
                        logger.info("All dolls are unavailable, checking down the list")

                        // Check if we actually scrolled down by comparing this subregion
                        val compareRegion = region.subRegion(120, 970, 265, 110)
                        val screenshot = compareRegion.takeScreenshot()

                        // Swipe down because all the dolls presented were in logistics
                        region.subRegion(140, 620, 1590, 455).randomLocation().let {
                            region.swipeRandomly(it, it.offset(0, Random.nextInt(-490, -480)), 1000)
                        }
                        delay(100)
                        // If it actually scrolled down then the region will have different contents
                        // from before
                        if (compareRegion.doesntHave(Image(screenshot, "SwipeCompare"))) continue
                    }
                    logger.info("No suitable doll that can be enhanced found")
                    // Click cancel
                    region.subRegion(120, 0, 205, 144).clickRandomly()
                    return
                } else {
                    doll.clickRandomly()
                    break
                }
            }

            delay(400)
            // Click "Select t-doll" button
            logger.info("Selecting T-dolls that will be used for enhancement")
            region.subRegion(760, 200, 1250, 550).find("factory/select.png").clickRandomly()
            delay(200)

            // Click smart select button
            logger.info("Using smart select")
            region.subRegion(1770, 859, 247, 158).clickRandomly(); yield()

            // Confirm doll selection
            val okButton = region.subRegion(1768, 859, 250, 158).findOrNull("factory/ok.png")
            if (okButton == null) {
                // Click cancel if no t dolls could be used for enhancement
                region.subRegion(120, 0, 205, 144).clickRandomly()
                logger.info("Stopping enhancement due to lack of 2 star T-dolls")
                break
            } else {
                okButton.clickRandomly()
                scriptStats.enhancementsDone += 1
            }

            delay(200)
            // Click enhance button
            region.subRegion(1763, 873, 250, 96).clickRandomly(); delay(300)
            // Click confirm if not enough T-dolls, got to get rid of the trash anyways :D
            region.findOrNull("confirm.png")?.clickRandomly(); yield()

            region.waitSuspending("close.png", 30)?.clickRandomly()
        }

        // Update stats after all the update jobs are complete
        launch {
            statUpdateJobs.forEach { it.join() }
            scriptStats.dollsUsedForEnhancement += dollsUsedForEnhancement.get()
            if (!gameState.dollOverflow) logger.info("The base now has space for new dolls")
        }
        // If disassembly is enabled then it will need to know the gamestate after enhancement
        // so we will need to wait for the update job to complete
        if (profile.factory.disassembly.enabled) statUpdateJobs.forEach { it.join() }
    }

    private suspend fun disassembleDolls() {
        logger.info("Doll limit reached, will try to disassemble")
        navigator.navigateTo(LocationId.TDOLL_DISASSEMBLY)

        var oldDollCount: List<String>? = null
        val dollsDisassembled = AtomicInteger(0)
        val statUpdateJobs = mutableListOf<Job>()

        logger.info("Disassembling 2 star T-dolls")
        while (isActive) {
            region.subRegion(483, 200, 1557, 565)
                    .waitSuspending("factory/select.png", 10)?.clickRandomly()
            delay(750)

            statUpdateJobs += updateJob(region.subRegion(1750, 810, 290, 70).takeScreenshot()) { count ->
                val c = count[0].toInt()
                oldDollCount?.get(0)?.toIntOrNull()?.let {
                    dollsDisassembled.getAndAdd(it - c)
                }
                oldDollCount = count
                c >= count[1].toInt()
            }

            // Click smart select button
            logger.info("Using smart select")
            region.subRegion(1770, 859, 247, 158).clickRandomly(); yield()

            // Confirm doll selection
            val okButton = region.subRegion(1768, 889, 250, 158).findOrNull("factory/ok.png")
            if (okButton == null) {
                // Click cancel if no t dolls could be used for enhancement
                region.subRegion(120, 0, 205, 144).clickRandomly()
                logger.info("No more 2 star T-dolls to disassemble!")
                break
            }
            // Click ok
            okButton.clickRandomly()
            // Click disassemble button
            region.subRegion(1749, 885, 247, 95).clickRandomly()
            // Update stats
            scriptStats.disassemblesDone += 1
        }

        var filtersApplied = false

        logger.info("Disassembling 3 star T-dolls")
        while (isActive) {
            region.subRegion(483, 200, 1557, 565)
                    .waitSuspending("factory/select.png", 10)?.clickRandomly()
            delay(750)

            statUpdateJobs += updateJob(region.subRegion(1750, 810, 290, 70).takeScreenshot()) { count ->
                val c = count[0].toInt()
                oldDollCount?.get(0)?.toIntOrNull()?.let {
                    dollsDisassembled.getAndAdd(it - c)
                }
                oldDollCount = count
                c >= count[1].toInt()
            }

            if (!filtersApplied) {
                logger.info("Applying filters")
                filtersApplied = true
                applyDollFilters(3)
            }
            delay(400)
            val dolls = region.findAllOrEmpty("doll-list/3star.png")
                    .also { logger.info("Found ${it.size} that can be disassembled") }
                    .map { region.subRegion(it.x - 102, it.y, 239, 427) }
            if (dolls.isEmpty()) {
                // Click cancel if no t dolls could be used for enhancement
                region.subRegion(120, 0, 205, 144).clickRandomly()
                logger.info("No more 3 star T-dolls to disassemble!")
                break
            }
            // Select all the dolls
            region.mouseDelay(0.0) {
                dolls.forEach { it.clickRandomly() }
            }
            // Click ok
            region.subRegion(1768, 889, 250, 158).find("factory/ok.png").clickRandomly(); yield()
            // Click disassemble button
            region.subRegion(1749, 885, 247, 95).clickRandomly(); delay(200)
            // Click confirm
            region.subRegion(1100, 688, 324, 161).find("confirm.png").clickRandomly(); yield()
            // Update stats
            scriptStats.disassemblesDone += 1
            // Wait for menu to settle
            region.subRegion(483, 200, 1557, 565).waitSuspending("factory/select.png", 20)
            // Can break if disassembled count is less than 12
            if (dolls.size < 12) break
        }
        // Update stats after all the update jobs are complete
        launch {
            statUpdateJobs.forEach { it.join() }
            scriptStats.dollsUsedForDisassembly += dollsDisassembled.get()
            if (!gameState.dollOverflow) logger.info("The base now has space for new dolls")
        }
    }

    private fun updateJob(screenshot: BufferedImage, action: (List<String>) -> Boolean): Job {
        return launch {
            Ocr.forConfig(config).doOCRAndTrim(screenshot)
                    .also { logger.info("Detected doll count: $it") }
                    .split(Regex("\\D"))
                    .let { currentDollCount ->
                        gameState.dollOverflow = try {
                            action(currentDollCount)
                        } catch (e: Exception) {
                            false
                        }
                    }
        }
    }
}