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

import com.waicool20.cvauto.core.input.ITouchInterface
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.core.template.ImageTemplate
import com.waicool20.cvauto.core.util.ColorSpaceUtils
import com.waicool20.wai2k.events.DollDisassemblyEvent
import com.waicool20.wai2k.events.DollEnhancementEvent
import com.waicool20.wai2k.events.EquipDisassemblyEvent
import com.waicool20.wai2k.events.EventBus
import com.waicool20.wai2k.game.location.LocationId
import com.waicool20.wai2k.script.Navigator
import com.waicool20.wai2k.util.loggerFor
import com.waicool20.wai2k.util.readText
import com.waicool20.wai2k.util.useCharFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.awt.Point
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

@Suppress("unused")
class FactoryModule(navigator: Navigator) : ScriptModule(navigator) {
    private val logger = loggerFor<FactoryModule>()

    override suspend fun execute() {
        checkDollOverflow()
        checkEquipOverflow()
    }

    private suspend fun checkDollOverflow() {
        if (!gameState.dollOverflow) return
        if (profile.factory.enhancement.enabled) enhanceDolls()

        if (gameState.dollOverflow && !profile.factory.disassembly.enabled) scriptRunner.stop("Doll limit reached")

        // Bypass overflow check if always disassemble
        if (!profile.factory.alwaysDisassembleAfterEnhance && !gameState.dollOverflow) return
        if (profile.factory.disassembly.enabled) disassembleDolls()

        if (gameState.dollOverflow) scriptRunner.stop("Doll limit reached")
    }

    private suspend fun checkEquipOverflow() {
        if (!gameState.equipOverflow) return
        if (profile.factory.equipDisassembly.enabled) disassembleEquip()
    }

    /**
     * Keeps enhancing dolls until there are no more 2-star dolls
     */
    private suspend fun enhanceDolls() {
        logger.info("Doll limit reached, will try to enhance")
        navigator.navigateTo(LocationId.TDOLL_ENHANCEMENT)

        var oldCount = 0

        suspend fun updateCount() {
            val (currentCount, _) = getCurrentCount(Count.DOLL)
            val countDelta = oldCount - currentCount
            if (countDelta > 0) EventBus.publish(
                DollEnhancementEvent(
                    countDelta,
                    sessionId,
                    elapsedTime
                )
            )
            oldCount = currentCount
        }

        while (coroutineContext.isActive) {
            val selectCharacterButton = region.subRegion(351, 206, 246, 605)
            // Click select character
            selectCharacterButton.click(); delay(1000)

            updateCount()

            logger.info("Selecting highest level T-doll for enhancement")
            // Randomly select a doll on the screen for enhancement
            while (coroutineContext.isActive) {
                val doll = // Map lock region to doll region
                    // Prioritize higher level dolls
                    region.findBest(FileTemplate("doll-list/lock.png"), 20)
                        .map { it.region }
                        .also { logger.info("Found ${it.size} dolls on screen") }
                        // Only select dolls that are available by checking the brightness of their lock
                        .filter {
                            val c = it.pickColor(18, 32)
                            val hsv = intArrayOf(c.red, c.green, c.blue)
                            ColorSpaceUtils.rgbToHsv(hsv)
                            hsv[2] > 128
                        }
                        .also { logger.info("With ${it.size} available for enhancement") }
                        // Map lock region to doll region
                        .map { region.subRegion(it.x - 7, it.y, 244, it.height) }
                        .minByOrNull { it.y * 10 + it.x }
                if (doll == null) {
                    if (region.findBest(FileTemplate("doll-list/logistics.png"), 20).size >= 12) {
                        logger.info("All dolls are unavailable, checking down the list")

                        // Check if we actually scrolled down by comparing this subregion
                        val compareRegion = region.subRegion(120, 970, 265, 110)
                        val screenshot = compareRegion.capture().img

                        val src = region.subRegion(140, 620, 1590, 455).randomPoint()
                        val dest =
                            Point(src.x, src.y).apply { translate(0, Random.nextInt(-490, -480)) }

                        // Swipe down because all the dolls presented were in logistics
                        region.device.input.touchInterface.swipe(
                            ITouchInterface.Swipe(
                                0, src.x, src.y, dest.x, dest.y
                            ), 1000
                        )

                        delay(100)
                        // If it actually scrolled down then the region will have different contents
                        // from before
                        if (compareRegion.doesntHave(ImageTemplate(screenshot))) continue
                    }
                    logger.info("No suitable doll that can be enhanced found")
                    // Click cancel
                    region.subRegion(0, 0, 205, 144).click()
                    return
                } else {
                    doll.click()
                    break
                }
            }

            delay(400)
            // Click "Select t-doll" button
            logger.info("Selecting T-dolls that will be used for enhancement")
            region.subRegion(678, 212, 1217, 555)
                .waitHas(FileTemplate("factory/enhance-select-tdoll.png"), 5000)?.click()
            delay(200)

            // Click smart select button
            logger.info("Using smart select")
            region.subRegion(1649, 899, 248, 156).click()
            delay(1000)

            // Confirm doll selection
            val okButton = region.subRegion(1642, 885, 268, 177)
                .findBest(FileTemplate("factory/ok.png"))?.region
            if (okButton == null) {
                // Click cancel if no t dolls could be used for enhancement
                region.subRegion(0, 0, 205, 144).click()
                logger.info("Stopping enhancement due to lack of 2 star T-dolls")
                break
            } else {
                logger.info("Confirm doll selections")
                okButton.click()
            }

            delay(200)
            // Click enhance button
            logger.info("Enhancing T-doll")
            region.subRegion(1643, 902, 247, 96).click()
            delay(300)

            // Click confirm if not enough T-dolls, got to get rid of the trash anyways :D
            region.subRegion(973, 668, 290, 150)
                .findBest(FileTemplate("ok.png"))?.region?.let {
                    logger.info("Not enough T-dolls for enhancement, but enhancing anyways")
                    it.click()
                }

            region.subRegion(678, 212, 1217, 555)
                .waitHas(FileTemplate("factory/enhance-select-tdoll.png"), 3000)
            delay(1000)
        }

        if (!gameState.dollOverflow) logger.info("The base now has space for new dolls")
    }

    private val disassemblyWindow by lazy { region.subRegion(335, 217, 1169, 771) }
    private val disassembleButton by lazy { region.subRegion(1582, 859, 247, 95) }

    private suspend fun disassembleDolls() {
        logger.info("Doll limit reached, will try to disassemble")
        navigator.navigateTo(LocationId.TDOLL_DISASSEMBLY)

        var oldCount = 0

        suspend fun updateCount() {
            val (currentCount, _) = getCurrentCount(Count.DOLL_D)
            val countDelta = oldCount - currentCount
            if (countDelta > 0) EventBus.publish(
                DollDisassemblyEvent(
                    countDelta,
                    sessionId,
                    elapsedTime
                )
            )
            oldCount = currentCount
        }

        logger.info("Disassembling 2 star T-dolls")

        val sTemp = FileTemplate("factory/disassemble-select-tdoll.png", 0.95)

        while (coroutineContext.isActive) {
            logger.info("Start T-doll selection")

            disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

            // If still on disassemble menu, maybe there's no more dolls to disassemble
            if (locations.getValue(LocationId.TDOLL_DISASSEMBLY).isInRegion(region)) {
                gameState.dollOverflow = false
                return
            }

            updateCount()
            // Click smart select button
            logger.info("Using smart select")
            region.subRegion(1646, 899, 254, 163).click()
            delay(400)

            // Confirm doll selection
            val okButton = region.subRegion(1644, 880, 268, 177)
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
        }

        updateCount()

        logger.info("Disassembling 3 star T-dolls")
        logger.info("Applying filters")
        applyDollFilters(listOf(3, 4))
        delay(750)

        while (coroutineContext.isActive) {
            val dolls = region.findBest(FileTemplate("doll-list/3star.png"), 12)
                .map { it.region }
                .also { logger.info("Found ${it.size} that can be disassembled") }
                .map { region.subRegion(it.x - 102, it.y, 136, 427) }
                .toMutableList()
            yield()
            if (profile.factory.disassembly.disassemble4Star) {
                dolls += region.findBest(FileTemplate("doll-list/4star.png"), 12)
                    .map { it.region }
                    .also { logger.info("Found ${it.size} 4*s that can be disassembled") }
                    .map { region.subRegion(it.x - 106, it.y - 3, 247, 436) }
            }
            if (dolls.isEmpty()) {
                // Click cancel if no t dolls could be used for disassembly
                region.subRegion(0, 0, 205, 144).click()
                break
            }
            // Select all the dolls
            dolls.sortedBy { it.y * 10 + it.x }.forEach {
                it.click()
                delay(250)
            }
            delay(250)
            logger.info("Confirm doll selections")
            // Click ok
            region.subRegion(1644, 880, 268, 177)
                .waitHas(FileTemplate("factory/ok.png"), 1500)?.click()
            logger.info("Disassembling selected T-dolls")
            disassembleButton.click(); delay(1000)
            // Click confirm
            region.subRegion(1000, 865, 324, 161)
                .findBest(FileTemplate("ok.png"))?.region?.click(); delay(200)
            // Update stats
            EventBus.publish(DollDisassemblyEvent(dolls.size, sessionId, elapsedTime))
            // Can break if disassembled count is less than 12
            if (dolls.size < 12) {
                logger.info("No more T-dolls to disassemble!")
                break
            } else {
                logger.info("Still more T-dolls to disassemble")
            }

            disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

            // If still on disassemble menu, maybe there's no more dolls to disassemble
            if (locations.getValue(LocationId.TDOLL_DISASSEMBLY).isInRegion(region)) {
                gameState.dollOverflow = false
                return
            }
        }
        if (!gameState.dollOverflow) logger.info("The base now has space for new dolls")
    }

    private suspend fun disassembleEquip() {
        logger.info("Equipment limit reached, will try to disassemble")
        navigator.navigateTo(LocationId.TDOLL_DISASSEMBLY)

        var oldCount = 0

        suspend fun updateCount() {
            val (currentCount, total) = getCurrentCount(Count.EQUIP)
            gameState.equipOverflow = currentCount < total
            val countDelta = oldCount - currentCount
            if (countDelta > 0) EventBus.publish(
                EquipDisassemblyEvent(
                    countDelta,
                    sessionId,
                    elapsedTime
                )
            )
            oldCount = currentCount
        }

        logger.info("Disassembling 2 star equipment")
        logger.info("Start equipment selection")

        val sTemp = FileTemplate("factory/disassemble-select-equip.png", 0.95)

        disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

        // If still on disassemble menu, maybe there's no more equips to disassemble
        if (locations.getValue(LocationId.TDOLL_DISASSEMBLY).isInRegion(region)) {
            gameState.equipOverflow = false
            return
        }

        updateCount()
        // Click smart select button
        logger.info("Using smart select")
        region.subRegion(1695, 899, 254, 163).click()
        delay(400)

        // Confirm doll selection
        val okButton = region.subRegion(1644, 880, 268, 177)
            .findBest(FileTemplate("factory/ok-equip.png"))?.region
        logger.info("Confirm equipment selections")
        // Click ok
        okButton?.click()
        disassemblyWindow.waitHas(sTemp, 3000)
        logger.info("Disassembling selected equipment")
        disassembleButton.click(); delay(750)

        disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

        // If still on disassemble menu, maybe there's no more equips to disassemble
        if (locations.getValue(LocationId.TDOLL_DISASSEMBLY).isInRegion(region)) {
            gameState.equipOverflow = false
            return
        }

        logger.info("Disassembling higher rarity equipment")
        logger.info("Applying filters")
        // Filter button
        region.subRegion(1662, 347, 257, 162).click(); delay(750)
        // 3 star
        region.subRegion(134, 213, 257, 117).click(); yield()
        if (profile.factory.equipDisassembly.disassemble4Star) {
            // 4 star
            logger.info("4 star disassembly is on")
            region.subRegion(1069, 213, 257, 117).click(); yield()
        }
        // Confirm
        region.subRegion(1200, 979, 413, 83).click(); yield()
        delay(750)

        updateCount()

        while (coroutineContext.isActive) {
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
            if (equips.isEmpty()) {
                // Click cancel if no equips could be used for disassembly
                region.subRegion(0, 0, 205, 144).click()
                break
            }
            // Select all equips
            equips.sortedBy { it.y * 10 + it.x }.forEach {
                it.click()
                delay(250)
            }
            logger.info("Confirm equipment selections")
            // Click ok
            region.subRegion(1644, 880, 268, 177)
                .findBest(FileTemplate("factory/ok-equip.png"))?.region?.click(); delay(500)
            logger.info("Disassembling selected equipment")
            disassembleButton.click(); delay(1000)
            // Click confirm
            region.subRegion(993, 865, 324, 161)
                .waitHas(FileTemplate("ok.png"), 1500)?.click()
            if (profile.factory.equipDisassembly.disassemble4Star) {
                // If there's both 3star and 4star in the same scrap pool, game will prompt twice
                region.subRegion(993, 865, 324, 161)
                    .waitHas(FileTemplate("ok.png"), 1500)?.click()
            }
            // Update stats
            EventBus.publish(EquipDisassemblyEvent(equips.size, sessionId, elapsedTime))
            // Can break if disassembled count is less than 12
            if (equips.size < 12) {
                logger.info("No more higher rarity equipment to disassemble!")
                gameState.equipOverflow = false
                break
            } else {
                logger.info("Still more higher rarity equipment to disassemble")
            }

            disassemblyWindow.waitHas(sTemp, 10000)?.click(); delay(750)

            // If still on disassemble menu, maybe there's no more equips to disassemble
            if (locations.getValue(LocationId.TDOLL_DISASSEMBLY).isInRegion(region)) {
                gameState.equipOverflow = false
                break
            }
        }
    }

    private enum class Count(val keyword: String, val yLabel: Int, val yCount: Int) {
        DOLL("capa", 790, 840),
        DOLL_D("capa", 763, 815),
        EQUIP("equip", 763, 815)
    }

    private tailrec suspend fun getCurrentCount(type: Count): Pair<Int, Int> {
        logger.info("Updating $type count")
        val countRegex = Regex("(\\d+)\\s*?/\\s*?(\\d+)")
        val countRegion = region.subRegion(1657, type.yCount, 240, 60)
        var ocrResult: String
        while (coroutineContext.isActive) {
            ocrResult = ocr.readText(countRegion.copy(y = type.yLabel), threshold = 0.72, invert = true)
            if (ocrResult.contains(type.keyword, true)) break else yield()
        }
        ocrResult = ocr.useCharFilter("0123456789/")
            .readText(countRegion, threshold = 0.72, invert = true)
        logger.info("$type count ocr: $ocrResult")
        return countRegex.find(ocrResult)?.groupValues?.let {
            val count = it[1].toInt()
            val total = it[2].toInt()
            gameState.dollOverflow = count >= total
            logger.info("Count: $count | Total: $total")
            count to total
        } ?: getCurrentCount(type)
    }
}
