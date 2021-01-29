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

import boofcv.alg.color.ColorHsv
import com.waicool20.cvauto.core.input.ITouchInterface
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.core.template.ImageTemplate
import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.binarizeImage
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import java.awt.Color
import java.awt.Point
import kotlin.random.Random

class FactoryModule(navigator: Navigator) : ScriptModule(navigator) {
    private val logger = loggerFor<FactoryModule>()

    override suspend fun execute() {
        checkDollOverflow()
        checkEquipOverflow()
    }

    private suspend fun checkDollOverflow() {
        if (!gameState.dollOverflow) return
        if (profile.factory.enhancement.enabled) enhanceDolls()

        if (gameState.dollOverflow && !profile.factory.disassembly.enabled) stopScript("Doll limit reached")

        // Bypass overflow check if always disassemble
        if (!profile.factory.alwaysDisassembleAfterEnhance && !gameState.dollOverflow) return
        if (profile.factory.disassembly.enabled) disassembleDolls()

        if (gameState.dollOverflow) stopScript("Doll limit reached")
    }

    private suspend fun checkEquipOverflow() {
        if (!gameState.equipOverflow) return
        if (profile.factory.equipDisassembly.enabled) disassembleEquip()
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
            try {
                withTimeout(5000) {
                    val (currentCount, _) = getCurrentDollCount()
                    oldCount?.let { scriptStats.dollsUsedForEnhancement += it - currentCount }
                    oldCount = currentCount
                }
            } catch (e: TimeoutCancellationException) {
                logger.warn("Timed out on doll count, cancelling enhancement")
                return
            }

            logger.info("Selecting highest level T-doll for enhancement")
            // Randomly select a doll on the screen for enhancement
            while (isActive) {
                region.matcher.settings.matchDimension = ScriptRunner.HIGH_RES
                val doll = // Map lock region to doll region
                    // Prioritize higher level dolls
                    region.findBest(FileTemplate("doll-list/lock.png"), 20)
                        .map { it.region }
                        .also { logger.info("Found ${it.size} dolls on screen") }
                        // Only select dolls that are available by checking the brightness of their lock
                        .filter {
                            val c = Color(it.capture().getRGB(18, 32))
                            val hsv = FloatArray(3)
                            ColorHsv.rgbToHsv(c.red.toFloat(), c.green.toFloat(), c.blue.toFloat(), hsv)
                            hsv[2] > 128
                        }
                        .also { logger.info("With ${it.size} available for enhancement") }
                        // Map lock region to doll region
                        .map { region.subRegion(it.x - 7, it.y, 244, it.height) }
                        .minByOrNull { it.y * 10 + it.x }
                region.matcher.settings.matchDimension = ScriptRunner.NORMAL_RES
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
            region.subRegion(798, 212, 1217, 555)
                .findBest(FileTemplate("factory/select.png"))?.region?.click()
            delay(200)

            // Click smart select button
            logger.info("Using smart select")
            region.subRegion(1768, 859, 250, 158).click()
            delay(400)

            // Confirm doll selection
            val okButton = region.subRegion(1759, 850, 268, 177)
                .findBest(FileTemplate("factory/ok.png"))?.region
            if (okButton == null) {
                // Click cancel if no t dolls could be used for enhancement
                region.subRegion(120, 0, 205, 144).click()
                logger.info("Stopping enhancement due to lack of 2 star T-dolls")
                break
            } else {
                logger.info("Confirm doll selections")
                okButton.click()
            }

            delay(200)
            // Click enhance button
            logger.info("Enhancing T-doll")
            region.subRegion(1763, 892, 250, 96).click()
            scriptStats.enhancementsDone += 1
            delay(300)

            // Click confirm if not enough T-dolls, got to get rid of the trash anyways :D
            region.subRegion(1095, 668, 290, 150)
                .findBest(FileTemplate("ok.png"))?.region?.let {
                    logger.info("Not enough T-dolls for enhancement, but enhancing anyways")
                    it.click()
                }

            region.subRegion(798, 212, 1217, 555)
                .waitHas(FileTemplate("factory/select.png"), 1000)
            delay(1000)
        }

        if (!gameState.dollOverflow) logger.info("The base now has space for new dolls")
    }

    private val disassemblyWindow = region.subRegion(454, 217, 1169, 771)
    private val disassembleButton = region.subRegion(1702, 859, 247, 95)

    private suspend fun disassembleDolls() {
        logger.info("Doll limit reached, will try to disassemble")
        navigator.navigateTo(LocationId.TDOLL_DISASSEMBLY)

        var oldCount: Int? = null

        logger.info("Disassembling 2 star T-dolls")

        val sTemp = FileTemplate("factory/select.png", 0.8)

        while (isActive) {
            logger.info("Start T-doll selection")

            disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

            // If still on disassemble menu, maybe there's no more dolls to disassemble
            if (GameLocation.mappings(config)[LocationId.TDOLL_DISASSEMBLY]?.isInRegion(region) == true) {
                gameState.dollOverflow = false
                return
            }

            // Find the old doll count
            try {
                withTimeout(5000) {
                    val (currentCount, _) = getCurrentDollCount()
                    oldCount?.let { scriptStats.dollsUsedForDisassembly += it - currentCount }
                    oldCount = currentCount
                }
            } catch (e: TimeoutCancellationException) {
                logger.warn("Timed out on doll count, cancelling disassemble")
                return
            }

            // Click smart select button
            logger.info("Using smart select")
            region.subRegion(1768, 890, 250, 158).click()
            delay(400)

            // Confirm doll selection
            val okButton = region.subRegion(1759, 880, 268, 177)
                .findBest(FileTemplate("factory/ok.png"))?.region
            if (okButton == null) {
                logger.info("No more 2 star T-dolls to disassemble!")
                break
            }
            logger.info("Confirm doll selections")
            // Click ok
            okButton.click()
            disassemblyWindow.waitHas(sTemp, 3000)
            logger.info("Disassembling selected T-dolls")
            // Click disassemble button
            disassembleButton.click(); delay(750)
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
            dolls.sortedBy { it.y * 10 + it.x }.forEach {
                it.click()
                delay(250)
            }
            delay(250)
            scriptStats.dollsUsedForDisassembly += dolls.size
            logger.info("Confirm doll selections")
            // Click ok
            region.subRegion(1759, 880, 268, 177)
                .waitHas(FileTemplate("factory/ok.png"), 1500)?.click()
            logger.info("Disassembling selected T-dolls")
            disassembleButton.click(); delay(500)
            // Click confirm
            region.subRegion(1100, 865, 324, 161)
                .findBest(FileTemplate("ok.png"))?.region?.click(); delay(200)
            // Update stats
            scriptStats.disassemblesDone += 1
            // Can break if disassembled count is less than 12
            if (dolls.size < 12) {
                logger.info("No more 3 star T-dolls to disassemble!")
                break
            } else {
                logger.info("Still more 3 star T-dolls to disassemble")
            }

            disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

            // If still on disassemble menu, maybe there's no more dolls to disassemble
            if (GameLocation.mappings(config)[LocationId.TDOLL_DISASSEMBLY]?.isInRegion(region) == true) {
                gameState.dollOverflow = false
                return
            }
        }
        if (!gameState.dollOverflow) logger.info("The base now has space for new dolls")
    }

    private suspend fun disassembleEquip() {
        logger.info("Equipment limit reached, will try to disassemble")
        navigator.navigateTo(LocationId.TDOLL_DISASSEMBLY)

        var oldCount: Int? = null
        logger.info("Disassembling 2 star equipment")
        logger.info("Start equipment selection")


        val sTemp = FileTemplate("factory/select-equip.png")

        disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

        // If still on disassemble menu, maybe there's no more equips to disassemble
        if (GameLocation.mappings(config)[LocationId.TDOLL_DISASSEMBLY]?.isInRegion(region) == true) {
            gameState.equipOverflow = false
            return
        }

        // Find the old equip count
        try {
            withTimeout(5000) {
                val (currentCount, _) = getCurrentEquipCount()
                oldCount?.let { scriptStats.equipsUsedForDisassembly += it - currentCount }
                oldCount = currentCount
            }
        } catch (e: TimeoutCancellationException) {
            logger.warn("Timed out on equipment count, cancelling disassemble")
            return
        }
        // Click smart select button
        logger.info("Using smart select")
        region.subRegion(1772, 894, 237, 163).click()
        delay(400)

        // Confirm doll selection
        val okButton = region.subRegion(1768, 889, 250, 170)
            .findBest(FileTemplate("factory/ok-equip.png"))?.region
        logger.info("Confirm equipment selections")
        // Click ok
        okButton?.click()
        disassemblyWindow.waitHas(sTemp, 3000)
        logger.info("Disassembling selected equipment")
        disassembleButton.click(); delay(750)
        // Update stats
        scriptStats.equipDisassemblesDone += 1

        disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

        // If still on disassemble menu, maybe there's no more equips to disassemble
        if (GameLocation.mappings(config)[LocationId.TDOLL_DISASSEMBLY]?.isInRegion(region) == true) {
            gameState.equipOverflow = false
            return
        }

        logger.info("Disassembling higher rarity equipment")
        logger.info("Applying filters")
        // Filter button
        region.subRegion(1767, 347, 257, 162).click(); delay(750)
        // 3 star
        region.subRegion(1460, 213, 257, 117).click(); yield()
        if (profile.factory.equipDisassembly.disassemble4Star) {
            // 4 star
            logger.info("4 star disassembly is on")
            region.subRegion(1190, 213, 257, 117).click(); yield()
        }
        // Confirm
        region.subRegion(1320, 979, 413, 83).click(); yield()
        delay(750)

        val (currentCount, total) = getCurrentEquipCount()
        oldCount?.let { scriptStats.equipsUsedForDisassembly += it - currentCount }

        while (isActive) {
            region.matcher.settings.matchDimension = ScriptRunner.HIGH_RES
            val equips = region.findBest(FileTemplate("factory/equip-3star.png"), 12)
                .map { it.region }
                .also { logger.info("Found ${it.size} 3*s that can be disassembled") }
                .map { region.subRegion(it.x - 106, it.y - 3, 247, 436) }
                .toMutableList()
            yield()
            if (profile.factory.equipDisassembly.disassemble4Star) {
                equips += region.findBest(FileTemplate("factory/equip-4star.png"), 12)
                    .map { it.region }
                    .also { logger.info("Found ${it.size} 4*s that can be disassembled") }
                    .map { region.subRegion(it.x - 106, it.y - 3, 247, 436) }
            }
            region.matcher.settings.matchDimension = ScriptRunner.NORMAL_RES
            if (equips.isEmpty()) {
                // Click cancel if no equips could be used for disassembly
                region.subRegion(120, 0, 205, 144).click()
                if (currentCount >= total) {
                    // This doesn't really apply anymore since 4* equip disassembly is implemented, but ehh SPEQ are a thing too
                    logger.info("Equipment capacity reached but could not disassemble anymore equipment, stopping script")
                    coroutineContext.cancelAndYield()
                } else break
            }
            // Select all equips
            equips.sortedBy { it.y * 10 + it.x }.forEach {
                it.click()
                delay(250)
            }
            scriptStats.equipsUsedForDisassembly += equips.size
            logger.info("Confirm equipment selections")
            // Click ok
            region.subRegion(1768, 889, 250, 170)
                .findBest(FileTemplate("factory/ok-equip.png"))?.region?.click(); delay(500)
            logger.info("Disassembling selected equipment")
            disassembleButton.click(); delay(500)
            // Click confirm
            region.subRegion(1100, 865, 324, 161)
                .waitHas(FileTemplate("ok.png"), 1500)?.click()
            if (profile.factory.equipDisassembly.disassemble4Star) {
                // If there's both 3star and 4star in the same scrap pool, game will prompt twice
                region.subRegion(1100, 865, 324, 161)
                    .waitHas(FileTemplate("ok.png"), 1500)?.click()
            }
            // Update stats
            scriptStats.equipDisassemblesDone += 1
            // Can break if disassembled count is less than 12
            if (equips.size < 12) {
                logger.info("No more higher rarity equipment to disassemble!")
                break
            } else {
                logger.info("Still more higher rarity equipment to disassemble")
            }

            disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

            // If still on disassemble menu, maybe there's no more equips to disassemble
            if (GameLocation.mappings(config)[LocationId.TDOLL_DISASSEMBLY]?.isInRegion(region) == true) {
                gameState.equipOverflow = false
                return
            }
        }
        if (!gameState.equipOverflow) logger.info("The base now has space for new equipment")
    }

    private val countRegex = Regex("(\\d+)\\s*?/\\s*?(\\d+)")
    private val countRegion = region.subRegion(1790, 815, 220, 60)

    private tailrec suspend fun getCurrentDollCount(): Pair<Int, Int> {
        logger.info("Updating doll count")
        var ocrResult: String
        while (isActive) {
            ocrResult = Ocr.forConfig(config).doOCRAndTrim(countRegion.copy(y = 763).capture().binarizeImage(0.7))
            if (ocrResult.contains("capa", true)) break else yield()
        }
        ocrResult = Ocr.forConfig(config).doOCRAndTrim(countRegion)
        ocrResult = Ocr.cleanNumericString(ocrResult)
        logger.info("Doll count ocr: $ocrResult")
        return countRegex.find(ocrResult)?.groupValues?.let {
            val count = it[1].toInt()
            val total = it[2].toInt()
            gameState.dollOverflow = count >= total
            logger.info("Count: $count | Total: $total")
            count to total
        } ?: getCurrentDollCount()
    }

    private tailrec suspend fun getCurrentEquipCount(): Pair<Int, Int> {
        logger.info("Updating equipment count")
        var ocrResult: String
        while (isActive) {
            ocrResult = Ocr.forConfig(config).doOCRAndTrim(countRegion.copy(y = 763))
            if (ocrResult.contains("equip", true)) break else yield()
        }
        ocrResult = Ocr.forConfig(config).doOCRAndTrim(countRegion)
        ocrResult = Ocr.cleanNumericString(ocrResult)
        logger.info("Equipment count ocr: $ocrResult")
        return countRegex.find(ocrResult)?.groupValues?.let {
            val count = it[1].toInt()
            val total = it[2].toInt()
            gameState.equipOverflow = count >= total
            logger.info("Count: $count | Total: $total")
            count to total
        } ?: getCurrentEquipCount()
    }
}