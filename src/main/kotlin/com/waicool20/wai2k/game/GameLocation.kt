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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.script.Asset
import kotlinx.coroutines.experimental.async
import org.sikuli.script.Pattern
import java.util.*

data class GameLocation(val name: String, val isIntermediate: Boolean = false) {
    val landmarks: List<Landmark> = emptyList()
    val links: List<Link> = emptyList()

    class Link(val dest: String, asset: Asset) {
        val asset = asset.apply { prefix = "locations/links/" }
        override fun toString() = "Link(dest=$dest)"
    }

    class Landmark(asset: Asset) {
        val asset = asset.apply { prefix = "locations/landmarks/" }
    }

    companion object {
        private var locations: Map<String, GameLocation> = emptyMap()
        fun listAll(wai2KConfig: Wai2KConfig, refresh: Boolean = false): List<GameLocation> {
            return if (refresh) {
                val file = wai2KConfig.assetsDirectory.resolve("locations/locations.json").toFile()
                jacksonObjectMapper().readValue<List<GameLocation>>(file).also { locations = it.associate { it.name to it } }
            } else locations.values.toList()
        }
    }

    /**
     * Extension function to check if a location is on screen
     *
     * @return True if location is on screen
     */
    suspend fun isInRegion(region: AndroidRegion): Boolean {
        if (landmarks.isEmpty()) return false
        return landmarks.map {
            async {
                it.asset.getSubRegionFor(region.androidScreen())
                        .exists(Pattern(it.asset.imagePath).exact(), 0.1) != null
            }
        }.all { it.await() }
    }

    fun shortestPathTo(dest: GameLocation): List<Pair<GameLocation, Link>> {
        if (this == dest) return emptyList()

        val visitedNodes = mutableMapOf<GameLocation, Pair<GameLocation, Link>?>()
        val queue = LinkedList<Pair<GameLocation, GameLocation?>>()

        queue.add(this to null)
        while (queue.isNotEmpty()) {
            val (currentNode, parent) = queue.poll()

            queue.addAll(currentNode.links.mapNotNull { locations[it.dest] }.map { it to currentNode })

            if (!visitedNodes.values.any { it?.first == currentNode }) {
                val link = parent?.links?.find { it.dest == currentNode.name }
                if (link != null) {
                    visitedNodes[parent] = currentNode to link
                }
            }

            if (currentNode == dest) break
        }

        return visitedNodes.values.filterNotNull()
    }
}