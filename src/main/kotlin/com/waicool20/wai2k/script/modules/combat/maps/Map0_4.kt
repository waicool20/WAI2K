/*
 * GPLv3 License
 *
 *  Copyright (c) WAI2K by joo
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

class Map0_4(
        scriptRunner: ScriptRunner,
        region: AndroidRegion,
        config: Wai2KConfig,
        profile: Wai2KProfile
) : MapRunner(scriptRunner, region, config, profile) {
    private val logger = loggerFor<Map0_4>()

    override suspend fun execute() {
        deployEchelons()
        region.find("combat/battle/start.png").clickRandomly()
        resupplyEchelons()
        planPath()
        switchAndRetreat()
        terminateBattle()
    }

    private suspend fun deployEchelons() {
        logger.info("Deploying echelon 1 to heliport")
        region.grow(5, 30, 0, 0).clickUntilGone("$PREFIX/heliport.png", 10)
        region.find("ok.png").clickRandomly()

        delay(200)

        logger.info("Deploying echelon 2 to command post")
        region.grow(0, 20, 0, 0).clickUntilGone("$PREFIX/commandpost.png", 10)
        region.find("ok.png").clickRandomly()

        delay(200)

        logger.info("Deploying echelon 3 to heliport 2")
        region.grow(0, 10, 40, 0).clickUntilGone("$PREFIX/heliport2.png", 10)
        region.find("ok.png").clickRandomly()

        delay(200)

        logger.info("Deployment complete")
    }

    private suspend fun resupplyEchelons() {
        logger.info("Resupplying echelon at command post")
        delay(3000)
        region.waitSuspending("$PREFIX/commandpost-deployed.png", 15)?.grow(0, 60, 10, 0)?.apply {
            clickRandomly(); yield()
            clickRandomly(); yield()
        } ?: error("Could not find command post")

        delay(200)
        region.clickUntilGone("combat/battle/resupply.png")

        region.findOrNull("close.png")?.clickRandomly()
        logger.info("Resupply complete")
    }

    private suspend fun planPath() {
        // Pan up
        region.subRegion(1033, 225, 240, 100).let {
            it.swipeToRandomly(it.offset(0, 700), 1500); yield()
        }

        delay(200)

        logger.info("Entering planning mode")
        region.clickUntilGone("combat/battle/plan.png")
        logger.info("Selecting echelon at heliport")
        region.find("$PREFIX/heliport-deployed.png").grow(0, 80, 100, 0)
                .clickRandomly(); yield()
        logger.info("Selecting node 1")
        region.find("$PREFIX/node1.png").grow(0, 60, 10, 0)
                .clickRandomly(); yield()
        logger.info("Selecting node 2")
        region.find("$PREFIX/node2.png").grow(0, 0, 60, 0)
                .clickRandomly(); yield()
        logger.info("Selecting node 3")
        region.find("$PREFIX/node3.png").grow(0, 80, 20, 0)
                .clickRandomly(); yield()
        logger.info("Selecting node 4")
        region.find("$PREFIX/node4.png").grow(0, 0, 70, 0)
                .clickRandomly(); yield()
        logger.info("Selecting node 5")
        region.find("$PREFIX/node5.png").grow(0, 0, 70, 0)
                .clickRandomly(); yield()
        logger.info("Executing plan")
        region.clickUntilGone("combat/battle/plan-execute.png")
    }

    private suspend fun switchAndRetreat() {
        logger.info("Waiting for battle to end")
        // Use a higher similarity threshold to prevent prematurely exiting the wait
        region.waitSuspending("$PREFIX/complete-condition.png", 600, 0.95)
        logger.info("Battle ended")


        logger.info("Selecting echelon 1 on node 5")
        region.find("$PREFIX/node5-deployed").grow(0, 0, 70, 0)
                .clickRandomly(); yield()
        logger.info("selecting dummy echelon in heliport 2")
        region.find("$PREFIX/heliport2-deployed").grow(0, 70, 40, 10)
                .clickRandomly(); yield()

        delay(300)

        logger.info("Switching echelon with dummy to retreat")
        region.find("combat/battle/switch.png").grow(10, 5, 0, 0)
                .clickRandomly(); yield()

        delay(1500)

        logger.info("retreating echelon 1")
        region.find("$PREFIX/heliport2-switched.png").grow(0, 50, 60, 0).apply {
            clickRandomly(); yield()
        }

        delay(300)
        region.clickUntilGone("combat/battle/retreat.png")

        delay(100)
        region.clickUntilGone("confirm.png")

        delay(200)

        logger.info("retreating echelon 2")
        region.find("$PREFIX/commandpost-retreat.png").grow(0, 80, 100, 0).apply {
            clickRandomly(); yield()
            clickRandomly(); yield()
            clickRandomly(); yield()
        }

        delay(300)
        region.clickUntilGone("combat/battle/retreat.png")

        delay(100)
        region.clickUntilGone("confirm.png")

        delay(200)
    }

    private suspend fun terminateBattle() {
        logger.info("Terminating Battle")
        region.find("combat/battle/initialTerminate.png").grow(0, 0, 0, 0)
                .clickRandomly(); yield()

        region.clickUntilGone("combat/battle/terminateConfirm")
    }
}

