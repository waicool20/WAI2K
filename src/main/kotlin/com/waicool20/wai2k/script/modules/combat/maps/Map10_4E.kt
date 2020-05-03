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
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.combat.MapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.random.Random

class Map10_4E(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map10_4E>()
    override val isCorpseDraggingMap = false
    override val extractBlueNodes = false
    override val extractYellowNodes = false

    override suspend fun execute() {
        if (gameState.requiresMapInit) {
            logger.info("Zoom out")
            repeat(2) {
                region.pinch(
                        Random.nextInt(700, 800),
                        Random.nextInt(300, 400),
                        0.0,
                        500
                )
                delay(200)
            }

            delay(1000)
        }



        val rEchelons = deployEchelons(nodes[0], nodes[1])
        //Heavyports are configured now
        gameState.requiresMapInit=false
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        //need to do something on empty supplies...selection doesn't work as intended
        resupplyEchelons(rEchelons + nodes[0])
        delay(1000)
        //lose focus of combat echelon
        nodes[2].findRegion().click()
        //resupplyEchelons(nodes[0])
        planPath()
        waitForTurnAndPoints(2,4,false)


        //Reset Map Zoom State
        repeat(1) {
            region.pinch(
                    Random.nextInt(700, 800),
                    Random.nextInt(300, 400),
                    0.0,
                    500
            )
            delay(200)
        }
        //Map gets moved a little so use another node to retreat
        nodes[4].findRegion().click()
        gameState.requiresMapInit = false
        delay(1000)
        retreatEchelons(nodes[4])
        terminateMission()
    }

    private suspend fun planPath() {
        logger.info("Selecting echelon at ${nodes[0]}")
        nodes[0].findRegion().click()

        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[2]}")
        nodes[2].findRegion().click()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click(); yield()

        logger.info("Selecting ${nodes[0]}")
        nodes[0].findRegion().click(); yield()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}
