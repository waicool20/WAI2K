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
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.combat.MapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.random.Random

class Map6_4N(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map6_4N>()
    override val isCorpseDraggingMap = false
    override val extractBlueNodes = false
    override val extractYellowNodes = false

    override suspend fun execute() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            zoom()
            gameState.requiresMapInit = false
            delay(500)
        }
        //Pinch for more consistent orientation
        zoom()
        delay(1000)

        val rEchelons = deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(rEchelons)
        planPath()
        waitForTurnAndPoints(2, 1, false)
        deployEchelons(nodes[4])
        swap()
        retreatEchelons(nodes[4])
        terminateMission()
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }

    private suspend fun zoom() {
        repeat(2) {
            region.pinch(
                    Random.nextInt(666, 777),
                    Random.nextInt(288, 404),
                    0.0,
                    500
            )
            delay(200)
        }
    }

    private suspend fun swap(){
        logger.info("Selecting Combat team at ${nodes[5]}")
        nodes[5].findRegion().click()

        logger.info("Selecting dummy at ${nodes[4]}")
        nodes[4].findRegion().click(); yield()


        val r = region.subRegionAs<AndroidRegion>(654, 484, 140, 50)
        /*      On second thought, no.
        r.waitHas(FileTemplate("combat/battle/switch.png", 0.8), 2000)
        */
        delay(2000)
        logger.info("Swapping")
        r.click()
        delay(2000)
    }
}
