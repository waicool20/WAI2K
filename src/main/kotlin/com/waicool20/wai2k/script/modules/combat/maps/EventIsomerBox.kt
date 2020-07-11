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

package com.waicool20.wai2k.script.modules.combat.maps

import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.cvauto.core.template.ImageTemplate
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.combat.MapNode
import com.waicool20.wai2k.util.cancelAndYield
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.awt.image.BufferedImage
import kotlin.random.Random


class EventIsomerBox(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : EventMapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<EventIsomerBox>()
    override val isCorpseDraggingMap = false
    override val extractBlueNodes = false

    override suspend fun enterMap() {
        // Delay needed after restart since it does some flash animations
        if (gameState.requiresMapInit) delay(5000)
        region.pinch(
                Random.nextInt(800, 900),
                Random.nextInt(100, 150),
                0.0,
                500
        )
        delay(500)
        logger.info("Scroll to beginning")
        val checkRegion = region.subRegion(550, 390, 600, 300)
        var checkImg: BufferedImage
        val left = region.subRegionAs<AndroidRegion>(567, 521, 40, 40)
        val right = left.copyAs<AndroidRegion>(x = left.x + 1050)
        while (isActive) {
            checkImg = checkRegion.capture()
            left.swipeTo(right)
            delay(750)
            if (checkRegion.has(ImageTemplate(checkImg))) {
                logger.info("At beginning")
                break
            }
        }
        repeat(3) { right.swipeTo(left) }
        region.matcher.settings.matchDimension = ScriptRunner.HIGH_RES
        region.subRegion(1306, 420, 853, 100)
                .findBest(FileTemplate("$PREFIX/map-entrance.png", 0.8))
                ?.region?.click() ?: error("Couldn't find map")
        delay(500)
        logger.info("Entering map")
        region.subRegion(1835, 597, 232, 111).click()
        region.waitHas(FileTemplate("combat/battle/plan.png"), 5000)
    }

    override suspend fun execute() {
        logger.info("Zoom out")
        region.pinch(
                Random.nextInt(800, 900),
                Random.nextInt(100, 150),
                0.0,
                500
        )
        delay(1500)
        deployEchelons(nodes[0], nodes[1])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])
        turn1a()
        delay(1000)
        waitForTurnAndPoints(1, 2, false)
        turn1b(); delay(2000)
        // planning pops up for a small amount of time
        // varying amount of battles and action points for next turn
        waitForTurnAssets(false, 0.9, "combat/battle/end.png", "combat/battle/plan.png")
        delay(1500)
        turn2()
        // we may or may not get attacked, if dummy at the right gets attacked lose the S rank
        // maybe figure out how to retreat them early
        waitForTurnAssets(false, 0.9, "combat/battle/end.png", "combat/battle/plan.png")
        delay(4000)
        // the flashing of this button makes it hard to get
        turn3()
        delay(2000)
        waitForTurnAssets(false, 0.9, "combat/battle/end.png", "combat/battle/plan.png")
        openEchelon(nodes[14])
        region.subRegionAs<AndroidRegion>(1170, 911, 376, 95).click() //Retrieval Complete
        handleBattleResults()
    }

    private suspend fun turn1a() {
        // Move team 1
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }

    private suspend fun turn1b() {
        // Swap team 1, deploy combat team 2 and auto plan to next turn
        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Selecting hostage at ${nodes[4]}")
        nodes[4].findRegion().click()

        logger.info("Switching hostage")
        region.waitHas(FileTemplate("combat/battle/switch.png"), 3000)?.click(); delay(2000)

        deployEchelons(nodes[5])
        delay(750)
        resupplyEchelons(nodes[5]) // they don't always need resupply

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[6]}")
        nodes[6].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }

    private suspend fun turn2() {
        // Maybe try zooming out to try and reuse nodes
        // Moves Team 1 down, deploys dummy, team 2 rescues a hostage
        logger.info("Selecting ${nodes[7]}")
        nodes[7].findRegion().click()

        logger.info("Selecting ${nodes[8]}")
        nodes[8].findRegion().click(); delay(2500)

        deployEchelons(nodes[9])
        delay(1000) // heli animation

        swapRescueHostage(nodes[10], nodes[11])

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[9]}")
        nodes[9].findRegion().click()

        region.waitHas(FileTemplate("combat/battle/move.png"), 3000)?.click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click(); delay(1000)

        waitForTurnAssets(true, 0.96, "combat/battle/plan.png")
        //sometimes it fails to cick end turn
        mapRunnerRegions.endBattle.click();delay(2000)
    }

    private suspend fun turn3() {
        logger.info("Zoom out")
        region.pinch(
                Random.nextInt(700, 800),
                Random.nextInt(250, 350),
                0.0,
                500
        )
        delay(2000)
        swapRescueHostage(nodes[12], nodes[13])

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click()

        region.waitHas(FileTemplate("combat/battle/move.png"), 3000)?.click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click(); delay(2000)
    }

    private suspend fun swapRescueHostage(team: MapNode, hostage: MapNode) {
        logger.info("Swapping and retrieving hostage at $hostage")
        team.findRegion().click(); delay(200)
        hostage.findRegion().click()

        logger.info("Swapping locations")
        region.waitHas(FileTemplate("combat/battle/switch.png"), 3000)?.click()
        delay(2000) // wait for switching animation
        team.findRegion().click() // team is now hostage

        logger.info("Rescuing Hostage at $team")
        region.waitHas(FileTemplate("combat/battle/rescue.png"), 3000)?.click(); delay(2000)
    }
}