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
import com.waicool20.wai2k.util.doOCRAndTrim
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

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
        if (profile.factory.disassembly.enabled) disassembleDolls()
    }

    /**
     * Keeps enhancing dolls until there are no more 2 star dolls
     */
    private suspend fun enhanceDolls() {
        logger.info("Doll limit reached, will try to enhance")
        val prefix = "factory/enhancement"
        navigator.navigateTo(LocationId.TDOLL_ENHANCEMENT)

        var oldDollCount: List<String>? = null
        val dollsUsedForEnhancement = AtomicInteger(0)
        val statUpdateJobs = mutableListOf<Job>()

        while (isActive) {
            val selectCharacterButton = region.subRegion(464, 189, 264, 497)
            // Click select character
            selectCharacterButton.clickRandomly(); delay(500)

            // Find the old doll count
            val screenshot = region.subRegion(1750, 810, 290, 70).takeScreenshot()
            statUpdateJobs += launch {
                Ocr.forConfig(config).doOCRAndTrim(screenshot)
                        .also { logger.info("Detected doll count: $it") }
                        .split(Regex("\\D"))
                        .let { currentDollCount ->
                            gameState.dollOverflow = try {
                                val c = currentDollCount[0].toInt()
                                oldDollCount?.get(0)?.toIntOrNull()?.let {
                                    dollsUsedForEnhancement.getAndAdd(it - c)
                                }
                                oldDollCount = currentDollCount
                                c >= currentDollCount[1].toInt()
                            } catch (e: Exception) {
                                false
                            }
                        }
            }

            logger.info("Selecting random T-doll for enhancement")
            // Randomly select a doll on the screen for enhancement
            region.findAllOrEmpty("doll-list/lock.png")
                    .also { logger.info("Found ${it.size} dolls on screen available for enhancement") }
                    // Map lock region to doll region
                    .map { region.subRegion(it.x - 7, it.y, 244, 164) }
                    // Prioritize higher level dolls
                    .sortedBy { it.y * 10 + it.x }
                    .firstOrNull()?.clickRandomly() ?: return

            // Click "Select t-doll" button
            logger.info("Selecting T-dolls that will be used for enhancement")
            region.subRegion(760, 200, 1250, 550).find("$prefix/select.png").clickRandomly()
            yield()

            // Click smart select button
            logger.info("Using smart select")
            region.subRegion(1770, 859, 247, 158).clickRandomly(); yield()

            // Confirm doll selection
            val okButton = region.subRegion(1768, 859, 250, 158).findOrNull("$prefix/ok.png")
            if (okButton == null) {
                // Click cancel if no t dolls could be used for enhancement
                region.subRegion(120, 0, 205, 144).clickRandomly()
                logger.info("Stopping enhancement due to lack of 2 star T-dolls")
                break
            } else {
                okButton.clickRandomly()
                scriptStats.enhancementsDone += 1
            }

            // Click enhance button
            region.subRegion(1763, 873, 250, 96).clickRandomly(); yield()
            // Click confirm if not enough T-dolls, got to get rid of the trash anyways :D
            region.findOrNull("confirm.png")?.clickRandomly(); yield()

            region.waitSuspending("close.png", 30)?.clickRandomly()
        }

        // Update stats after all the update jobs are complete
        launch {
            statUpdateJobs.forEach { it.join() }
            scriptStats.dollsUsedForEnhancement = dollsUsedForEnhancement.get()
            if (!gameState.dollOverflow) logger.info("The base now has space for new dolls")

        }
        // If disassembly is enabled then it will need to know the gamestate after enhancement
        // so we will need to wait for the update job to complete
        if (profile.factory.disassembly.enabled) statUpdateJobs.forEach { it.join() }
    }

    private suspend fun disassembleDolls() {
        logger.info("Work In Progress - Disassembly not available")
        return
        navigator.navigateTo(LocationId.TDOLL_DISASSEMBLY)
    }
}