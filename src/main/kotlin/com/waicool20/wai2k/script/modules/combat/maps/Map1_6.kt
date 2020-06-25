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

class Map1_6(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map1_6>()
    override val isCorpseDraggingMap = false
    override val extractBlueNodes = false
    override val extractYellowNodes = false

    override suspend fun execute() {
        logger.info("Zoom out")
        region.pinch(
                Random.nextInt(700, 800),
                Random.nextInt(250, 340),
                0.0,
                500
        )
        //Map to settle
        delay(1000)

        deployEchelons(nodes[0])
        mapRunnerRegions.startOperation.click(); yield()
        waitForGNKSplash()
        resupplyEchelons(nodes[0]) //Force resupply so echelons with no doll in slot 2 can run
        planPathFirst()
        waitForTurnAssets(false, 0.96,"combat/battle/plan.png")
        delay(1000)
        mapRunnerRegions.endBattle.click(); yield()
        waitForTurnAndPoints(3,3, false) //SF may be on the Heliport
        resupplyEchelons(nodes[2]) // might be >5 battles
        planPathSecond()
        waitForTurnAssets(true, 0.96,"combat/battle/plan.png")
        handleBattleResults()
    }

    private suspend fun planPathFirst() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[1]}")
        nodes[1].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }

    private suspend fun planPathSecond() {
        logger.info("Entering planning mode")
        mapRunnerRegions.planningMode.click(); yield()

        logger.info("Selecting ${nodes[3]}")
        nodes[3].findRegion().click()

        logger.info("Executing plan")
        mapRunnerRegions.executePlan.click()
    }
}
