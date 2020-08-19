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

import com.waicool20.cvauto.android.AndroidRegion

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
     * Pause button that appears at the top of the screen when engaging enemies
     */
    val pauseButton = region.subRegion(1020, 0, 110, 50)

    /**
     * Region to click when battle ends
     */
    val battleEndClick = region.subRegion(992, 24, 1100, 121)

    /**
     * Match window
     */
    val window = region.subRegion(455, 151, 1281, 929)

    /**
     * Button to open terminate mission menu
     */
    val terminateMenu = region.subRegion(398, 12, 156, 111)

    /**
     * Terminate mission button
     */
    val terminate = region.subRegion(1189, 694, 237, 88)

    /**
     * Restart mission button
     */
    val restart = region.subRegion(737, 694, 237, 88)

    /**
     * Retreat button
     */
    val retreat = region.subRegion(1471, 908, 250, 96)

    /**
     * Choose echelon button on heavy heliport
     */
    val chooseEchelon = region.subRegion(835, 86, 372, 101)

    /**
     * Select Operation button to return to combat menu
     */
    val selectOperation = region.subRegion(12, 15, 190, 110)

    /**
     * Retreat from Combat
     */
    val retreatCombat = region.subRegion(620, 20, 190, 60)
}