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

package com.waicool20.wai2k.game

import com.waicool20.wai2k.android.AndroidRegion

class MapRunnerRegions(region: AndroidRegion) {
    /**
     * 'End Round' button on the bottom right corner
     */
    val endBattle = region.subRegion(1884, 929, 242, 123)

    /**
     * Button that toggles planning mode
     */
    val planningMode = region.subRegion(0, 857, 214, 60)

    /**
     * Button that executes planned path
     */
    val executePlan = region.subRegion(1894, 921, 250, 131)

    /**
     * Resupply button when deploying echelons
     */
    val resupply = region.subRegion(1742, 793, 297, 96)

    /**
     * OK button that deploys the echelon
     */
    val deploy = region.subRegion(1772, 929, 224, 83)

    /**
     * Start operation button that starts the battle in the beginning
     */
    val startOperation = region.subRegion(1737, 905, 407, 158)

    /**
     * Terminate mission button that offers a restart or terminate option
     */
    val terminate = region.subRegion(411, 12, 110, 110)

    /**
     *  Terminate button that returns to the home screen when triggered
     */
    val terminateToHome = region.subRegion(1206, 699, 200, 80)

    /**
     * Retreat button to retreat the echelon
     */
    val retreat = region.subRegion(1505, 932, 215, 80)

    /**
     * Button that confirms an action
     */
    val confirm = region.subRegion(1131, 723, 240, 90)
}