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

import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptRunner
import com.waicool20.wai2k.script.modules.combat.MapRunner
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

class Map0_2(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map0_2>()

    override suspend fun execute() {
        deployEchelons()
        region.find("combat/battle/start.png").clickRandomly()
        resupplyEchelons()
        planPath()
        waitForTurn()
        headToCommandPost()
        waitForBattleEnd()
        handleBattleResults()
    }

    private suspend fun deployEchelons() {
        logger.info("Deploying echelon 1 to command post")
        region.grow(0, 60, 0, 0).clickUntilGone("$PREFIX/commandpost.png", 10)
        region.find("ok.png").clickRandomly()

        delay(200)

        logger.info("Deploying echelon 2 to heliport")
        region.clickUntilGone("$PREFIX/heliport.png", 10)
        region.find("ok.png").clickRandomly()

        delay(200)

        logger.info("Deployment complete")
    }

    private suspend fun resupplyEchelons() {
        logger.info("Resupplying echelon at heliport")
        delay(3000)
        region.waitSuspending("$PREFIX/heliport-deployed.png", 15)?.grow(0, 135, 0, 10)?.apply {
            clickRandomly(); yield()
            clickRandomly(); yield()
        } ?: error("Could not find heliport")

        delay(200)
        region.clickUntilGone("combat/battle/resupply.png")

        region.findOrNull("close.png")?.clickRandomly()
        logger.info("Resupply complete")
    }

    private suspend fun planPath() {
        logger.info("Entering planning mode")
        region.clickUntilGone("combat/battle/plan.png")

        logger.info("Selecting echelon at command post")
        region.find("$PREFIX/commandpost-deployed.png").grow(20, 0, 100, 0)
                .clickRandomly(); yield()

        logger.info("Selecting node 1")
        region.find("$PREFIX/node1.png").grow(0, 0, 40, 0)
                .clickRandomly(); yield()

        // Pan up
        region.subRegion(1033, 225, 240, 100).let {
            it.swipeToRandomly(it.offset(0, 1000), 1500); yield()
        }

        logger.info("Selecting node 2")
        region.find("$PREFIX/node2.png").grow(0, 40, 40, 0)
                .clickRandomly(); yield()

        logger.info("Selecting node 3")
        region.find("$PREFIX/node3.png").grow(10, 0, 0, 0)
                .clickRandomly(); yield()

        logger.info("Selecting node 4")
        region.find("$PREFIX/node4.png").grow(0, 30, 20, 0)
                .clickRandomly(); yield()

        logger.info("Executing plan")
        region.clickUntilGone("combat/battle/plan-execute.png")
    }

    private suspend fun waitForTurn() {
        logger.info("Waiting for turn to end")
        // Use a higher similarity threshold to prevent prematurely exiting the wait
        region.waitSuspending("$PREFIX/turn1CompletionCheck.png", 600, 0.95)
        logger.info("Out of Turns, Ending Battle")

        region.clickUntilGone("combat/battle/end.png", 15, 0.75)
    }

    private suspend fun headToCommandPost() {
        logger.info("Waiting for enemy turn to end")
        region.waitSuspending("$PREFIX/secondTurnStart.png", 600, 0.60)

        //adding a delay here to make sure the turn transition is complete
        delay(4000)

        logger.info("Entering planning mode")
        region.clickUntilGone("combat/battle/plan.png")

        logger.info("Selecting echelon at node 4")
        region.find("$PREFIX/node4deployed.png").grow(0, 0, 80, 0)
                .clickRandomly(); yield()

        logger.info("Selecting node 5")
        region.find("$PREFIX/node5.png").grow(0, 0, 40, 0)
                .clickRandomly(); yield()

        logger.info("Selecting node 6")
        region.find("$PREFIX/node6.png").grow(10, 10, 70, 10)
                .clickRandomly(); yield()

        logger.info("Executing plan")
        region.clickUntilGone("combat/battle/plan-execute.png")
    }
}