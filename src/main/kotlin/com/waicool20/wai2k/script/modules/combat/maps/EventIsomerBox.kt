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

import com.sun.media.jfxmedia.logging.Logger
import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.combat.MapNode
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.io.File
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
        // The mission menu will change after restarts and some other stuff prob
        region.pinch(
                Random.nextInt(800, 900),
                Random.nextInt(100, 150),
                0.0,
                500
        )
        // upon first entering to will scroll you to the right to the highest map you have unlocked?
        var retries = 4
        while(isActive) {
            val entrance = region.waitHas(FileTemplate("$PREFIX/map-entrance.png"), 3000)
            if (entrance != null) {
                entrance.click()
                }
            else {
                region.subRegionAs<AndroidRegion>(1900, 500, 30, 30)
                        .swipeTo(region.subRegionAs<AndroidRegion>(1900+1200, 500, 30, 30))
                retries -= 1
            }
            if(retries <= 0) {
                logger.info("Left Search unsuccessful")
                // maybe go right again
                break
            }
        }
        delay(1000)  // small animation
        region.subRegionAs<AndroidRegion>(1835, 599, 232, 109).click() // Confirm start
        val intelPointsLimited = region.waitHas(FileTemplate("ok.png"), 1000)
        if(intelPointsLimited != null){
            logger.info("All intel points have been gained for today")
            intelPointsLimited.click()
        }
    }

    override suspend fun execute() {
        logger.info("Zoom out")
        region.pinch(
                Random.nextInt(800, 900),
                Random.nextInt(100, 150),
                0.0,
                500
        )
        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0])
        turn1a()
        waitForTurnAndPoints(1,2)
        turn1b()
        // if planning mode button exists,  plan has ended
        // varying amount of battles and action points for next turn
        waitForTurnAssets(false, 0.96, "combat/battle/plan.png")
        turn2()
        // we may or may not get attacked, if dummy at the right gets attacked lose the S rank
        // maybe figure out how to retreat them early
        waitForTurnAssets(false, 0.9, "combat/battle/end.png")
        // the flashing of this button makes it hard to get
        turn3()
        waitForTurnAssets(false, 0.96, "combat/battle/plan.png")
        openEchelon(nodes[14])
        region.subRegionAs<AndroidRegion>(1170, 911, 376, 95).click() //Retrieval Complete
        handleBattleResults()
    }

    private suspend fun turn1a() {
        // Move team 1
        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click()

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
        nodes[8].findRegion().click()

        deployEchelons(nodes[9])

        swapRescueHostage(nodes[10], nodes[11])

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[9]}")
        nodes[9].findRegion().click()

        region.waitHas(FileTemplate("combat/battle/move.png"), 3000)?.click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()

        //no battles on the way back
        region.waitHas(FileTemplate("combat/battle/move.png"), 5000)

        logger.info("Ending turn")
        mapRunnerRegions.endBattle.click(); delay(1000)
    }

    private suspend fun turn3() {
        swapRescueHostage(nodes[12], nodes[13])

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click()

        region.waitHas(FileTemplate("combat/battle/move.png"), 3000)?.click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
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