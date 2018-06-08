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

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.wai2k.android.AndroidRegion
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.script.Asset
import com.waicool20.waicoolutils.logging.loggerFor
import kotlinx.coroutines.experimental.async
import org.sikuli.script.Pattern
import java.util.*

/**
 * Represents a location in the game
 *
 * @param id Id of this location
 * @param isIntermediate If this is intermediate then that means this location
 * can potentially be skipped to get to a destination when traversing a path.
 */
data class GameLocation(val id: LocationId, val isIntermediate: Boolean = false) {
    /**
     * List of [Landmark] of this [GameLocation], if all landmarks are present on screen
     * then the game location is on screen
     */
    val landmarks: List<Landmark> = emptyList()
    /**
     * List of available links to other destinations
     */
    val links: List<Link> = emptyList()

    /**
     * Represents a link to another game location
     *
     * @param dest Destination of this link
     * @param asset The corresponding asset that should be clicked to get to [dest]
     */
    class Link(val dest: LocationId, asset: Asset) {
        val asset = asset.apply { prefix = "locations/links/" }
        override fun toString() = "Link(dest=$dest)"
    }

    /**
     * Represents a landmark and its corresponding asset
     *
     * @param asset Asset of this landmark
     */
    class Landmark(asset: Asset) {
        val asset = asset.apply { prefix = "locations/landmarks/" }
    }

    companion object Loader {
        private var locations: Map<LocationId, GameLocation> = emptyMap()
        private val mapper = jacksonObjectMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        private var loaderLogger = loggerFor<Loader>()
        /**
         * Reads the locations list file with the assets directory read from the configuration
         *
         * @param wai2KConfig Configuration
         * @param refresh If true, locations will be read from file instead of cached values
         */
        fun mappings(wai2KConfig: Wai2KConfig, refresh: Boolean = false): Map<LocationId, GameLocation> {
            return if (refresh) {
                val file = wai2KConfig.assetsDirectory.resolve("locations/locations.json").toFile()
                mapper.readValue<List<GameLocation>>(file).associate { it.id to it }.also {
                    loaderLogger.info("Loaded ${it.size} location entries")
                    locations = it
                }
            } else locations
        }
    }

    /**
     * Function to check if a location is on screen
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

    /**
     * Finds the shortest path to some destination
     *
     * @param dest Destination
     * @return List of Pairs that contains in order the next location and corresponding link that
     * need to be traversed to get to destination
     */
    fun shortestPathTo(dest: GameLocation): List<Pair<GameLocation, Link>> {
        if (this == dest) return emptyList()

        val visitedNodes = mutableSetOf<GameLocation>()
        val path = mutableMapOf<GameLocation, Pair<GameLocation, Link>?>()
        val queue = LinkedList<Pair<GameLocation, GameLocation?>>()

        queue.add(this to null)
        while (queue.isNotEmpty()) {
            val (currentNode, parent) = queue.poll()

            // Add unvisited nodes presented by the current nodes links to the queue
            currentNode.links.mapNotNull { locations[it.dest] }.filterNot {
                visitedNodes.contains(it)
            }.map { it to currentNode }.let { queue.addAll(it) }

            // Add current node to path if a link exists
            if (!path.values.any { it?.first == currentNode }) {
                val link = parent?.links?.find { it.dest == currentNode.id }
                if (link != null) path[parent] = currentNode to link
            }

            visitedNodes.add(currentNode)
            if (currentNode == dest) break
        }

        return path.values.filterNotNull()
    }
}