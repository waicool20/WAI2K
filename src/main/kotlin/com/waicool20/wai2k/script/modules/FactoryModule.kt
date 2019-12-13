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

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.input.ITouchInterface
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.core.template.ImageTemplate
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import java.awt.Point
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
        // Bypass overflow check if always disassemble
        if (!profile.factory.alwaysDisassembleAfterEnhance && !gameState.dollOverflow) return
        if (profile.factory.disassembly.enabled) disassembleDolls()
    }

    /**
     * Keeps enhancing dolls until there are no more 2 star dolls
     */
    private suspend fun enhanceDolls() {
        logger.info("Doll limit reached, will try to enhance")
        navigator.navigateTo(LocationId.TDOLL_ENHANCEMENT)

        var oldCount: Int? = null

        while (isActive) {
            val selectCharacterButton = region.subRegion(468, 206, 246, 605)
            // Click select character
            selectCharacterButton.click(); delay(1000)

            // Find the old doll count
            val (currentCount, _) = getCurrentDollCount()
            oldCount?.let { scriptStats.dollsUsedForEnhancement += it - currentCount }
            oldCount = currentCount

            logger.info("Selecting highest level T-doll for enhancement")
            // Randomly select a doll on the screen for enhancement
            while (isActive) {
                val doll = // Map lock region to doll region
                        // Prioritize higher level dolls
                        region.findBest(FileTemplate("doll-list/lock.png"), 20)
                                .map { it.region }
                                .also { logger.info("Found ${it.size} dolls on screen available for enhancement") }
                                // Map lock region to doll region
                                .map { region.subRegion(it.x - 7, it.y, 244, it.height) }
                                .minBy { it.y * 10 + it.x }
                if (doll == null) {
                    if (region.findBest(FileTemplate("doll-list/logistics.png"), 20).size >= 12) {
                        logger.info("All dolls are unavailable, checking down the list")

                        // Check if we actually scrolled down by comparing this subregion
                        val compareRegion = region.subRegion(120, 970, 265, 110)
                        val screenshot = compareRegion.capture()

                        val src = region.subRegion(140, 620, 1590, 455).randomPoint()
                        val dest = Point(src.x, src.y).apply { translate(0, Random.nextInt(-490, -480)) }

                        // Swipe down because all the dolls presented were in logistics
                        region.device.input.touchInterface?.swipe(ITouchInterface.Swipe(
                                0, src.x, src.y, dest.x, dest.y
                        ), 1000)

                        delay(100)
                        // If it actually scrolled down then the region will have different contents
                        // from before
                        if (compareRegion.doesntHave(ImageTemplate(screenshot))) continue
                    }
                    logger.info("No suitable doll that can be enhanced found")
                    // Click cancel
                    region.subRegion(120, 0, 205, 144).click()
                    return
                } else {
                    doll.click()
                    break
                }
            }

            delay(400)
            // Click "Select t-doll" button
            logger.info("Selecting T-dolls that will be used for enhancement")
            region.subRegion(760, 217, 1250, 550).findBest(FileTemplate("factory/select.png"))?.region?.click()
            delay(200)

            // Click smart select button
            logger.info("Using smart select")
            region.subRegion(1770, 862, 247, 158).click(); yield()

            // Confirm doll selection
            val okButton = region.subRegion(1768, 859, 250, 158)
                    .findBest(FileTemplate("factory/ok.png"))?.region
            if (okButton == null) {
                // Click cancel if no t dolls could be used for enhancement
                region.subRegion(120, 0, 205, 144).click()
                logger.info("Stopping enhancement due to lack of 2 star T-dolls")
                break
            } else {
                okButton.click()
                scriptStats.enhancementsDone += 1
            }

            delay(200)
            // Click enhance button
            logger.info("Enhancing T-doll")
            region.subRegion(1763, 892, 250, 96).click()
            delay(300)

            // Click confirm if not enough T-dolls, got to get rid of the trash anyways :D
            region.subRegion(1095, 668, 290, 150)
                    .findBest(FileTemplate("ok.png"))?.region?.let {
                logger.info("Not enough T-dolls for enhancement, but enhancing anyways")
                it.click()
            }

            region.waitHas(FileTemplate("close.png"), 30000)?.click()
        }

        if (!gameState.dollOverflow) logger.info("The base now has space for new dolls")
    }

    private suspend fun disassembleDolls() {
        logger.info("Doll limit reached, will try to disassemble")
        navigator.navigateTo(LocationId.TDOLL_DISASSEMBLY)

        var oldCount: Int? = null

        logger.info("Disassembling 2 star T-dolls")
        while (isActive) {
            region.subRegion(483, 200, 1557, 565)
                    .waitHas(FileTemplate("factory/select.png"), 10)?.click()
            delay(750)

            // Find the old doll count
            val (currentCount, _) = getCurrentDollCount()
            oldCount?.let { scriptStats.dollsUsedForDisassembly += it - currentCount }
            oldCount = currentCount

            // Click smart select button
            logger.info("Using smart select")
            region.subRegion(1770, 890, 247, 158).click()
            delay(200)

            // Confirm doll selection
            val okButton = region.subRegion(1768, 889, 250, 158)
                    .findBest(FileTemplate("factory/ok.png"))?.region
            if (okButton == null) {
                logger.info("No more 2 star T-dolls to disassemble!")
                break
            }
            // Click ok
            okButton.click(); delay(500)
            // Click disassemble button
            region.subRegion(1749, 885, 247, 95).click()
            // Update stats
            scriptStats.disassemblesDone += 1
        }

        logger.info("Disassembling 3 star T-dolls")
        logger.info("Applying filters")
        applyDollFilters(3)
        delay(750)

        val (currentCount, _) = getCurrentDollCount()
        oldCount?.let { scriptStats.dollsUsedForDisassembly += it - currentCount }

        while (isActive) {
            region.matcher.settings.matchDimension = ScriptRunner.HIGH_RES
            val dolls = region.findBest(FileTemplate("doll-list/3star.png"), 12)
                    .map { it.region }
                    .also { logger.info("Found ${it.size} that can be disassembled") }
                    .map { region.subRegion(it.x - 102, it.y, 136, 427) }
            region.matcher.settings.matchDimension = ScriptRunner.NORMAL_RES
            if (dolls.isEmpty()) {
                // Click cancel if no t dolls could be used for disassembly
                region.subRegion(120, 0, 205, 144).click()
                break
            }
            // Select all the dolls
            dolls.sortedBy { it.y * 10 + it.x }.forEach { it.click() }
            scriptStats.dollsUsedForDisassembly += dolls.size
            // Click ok
            region.subRegion(1768, 889, 250, 158)
                    .findBest(FileTemplate("factory/ok.png"))?.region?.click(); delay(250)
            // Click disassemble button
            region.subRegion(1749, 885, 247, 95).click(); delay(250)
            // Click confirm
            region.subRegion(1100, 865, 324, 161)
                    .findBest(FileTemplate("ok.png"))?.region?.click(); delay(200)
            // Update stats
            scriptStats.disassemblesDone += 1
            // Can break if disassembled count is less than 12
            if (dolls.size < 12) break
            // Wait for menu to settle
            region.subRegion(483, 200, 1557, 565)
                    .waitHas(FileTemplate("factory/select.png"), 10)?.let {
                        it.click()
                        delay(750)
                    }
        }

        logger.info("No more 3 star T-dolls to disassemble!")
        if (!gameState.dollOverflow) logger.info("The base now has space for new dolls")
    }

    private tailrec fun getCurrentDollCount(): Pair<Int, Int> {
        val dollCountRegion = region.subRegion(1750, 750, 300, 150)
        var ocrResult = ""
        while(isActive) {
            ocrResult = Ocr.forConfig(config).doOCRAndTrim(dollCountRegion)
            if (ocrResult.contains("capacity", true)) break
        }
        return Regex("(\\d+)/(\\d+)").find(ocrResult)?.groupValues?.let {
            val count = it[1].toInt()
            val total = it[2].toInt()
            gameState.dollOverflow = count >= total
            count to total
        } ?: getCurrentDollCount()
    }
}