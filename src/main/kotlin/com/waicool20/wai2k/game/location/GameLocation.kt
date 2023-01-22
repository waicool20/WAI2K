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

package com.waicool20.wai2k.game.location

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.cvauto.core.AnyRegion
import com.waicool20.cvauto.core.template.FT
import com.waicool20.wai2k.config.Wai2kConfig
import com.waicool20.wai2k.script.Asset
import com.waicool20.wai2k.script.InvalidLocationsJsonFileException
import com.waicool20.wai2k.script.PathFindingException
import com.waicool20.wai2k.util.loggerFor
import java.util.*

/**
 * Represents a location in the game
 *
 * @param id ID of this location
 */
data class GameLocation(val id: LocationId) {
    enum class Mode {
        AND, OR
    }

    companion object Loader {
        private var locations: Map<LocationId, GameLocation> = emptyMap()
        private val mapper = jacksonMapperBuilder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build()
        private var loaderLogger = loggerFor<Loader>()

        /**
         * Reads the locations list file with the assets directory read from the configuration
         *
         * @param wai2KConfig Configuration
         * @param refresh If true, locations will be read from file instead of cached values
         */
        fun mappings(
            wai2KConfig: Wai2kConfig,
            refresh: Boolean = false
        ): Map<LocationId, GameLocation> {
            return if (refresh) {
                val file = wai2KConfig.assetsDirectory.resolve("locations/locations.json").toFile()
                mapper.readValue<List<GameLocation>>(file).associateBy { it.id }.also {
                    loaderLogger.info("Loaded ${it.size} location entries")
                    locations = it
                }
            } else locations
        }

        fun find(wai2KConfig: Wai2kConfig, location: LocationId): GameLocation {
            return mappings(wai2KConfig)[location] ?: throw InvalidLocationsJsonFileException()
        }
    }

    /**
     * List of landmark [Asset] of this [GameLocation], a landmark asset is an important
     * identifier on the screen that indicates whether we are at the current game location
     */
    val landmarks: List<Asset> = emptyList()

    /**
     * List of available links to other destinations
     */
    val links: List<Link> = emptyList()

    /**
     * Matching mode determines how landmarks are used to determine whether we are at the current
     * game location.
     *
     * With [Mode.AND] (the default), all landmarks must be on screen before the
     * game location is determined to be on screen.
     *
     * With [Mode.OR], any landmark can be on screen before the game location is determined to be
     * on screen
     */
    val matchingMode: Mode = Mode.AND

    /**
     * Function to check if a location is on screen
     *
     * @return True if location is on screen
     */
    fun isInRegion(region: AnyRegion): Boolean {
        if (landmarks.isEmpty()) return false
        return when (matchingMode) {
            Mode.AND -> landmarks.all {
                it.getSubRegionFor(region).has(FT(it.path, it.threshold))
            }
            Mode.OR -> landmarks.any {
                it.getSubRegionFor(region).has(FT(it.path, it.threshold))
            }
        }
    }

    /**
     * Wrapper class describing a node in a path
     *
     * @param source Source location
     * @param dest Destination location
     * @param link Link needed to traverse from source to dest
     */
    data class PathNode(val source: GameLocation, val dest: GameLocation, val link: Link)

    /**
     * Finds the shortest path to some destination
     *
     * @param dest Destination
     * @return List of [PathNode], null if no path solution is found
     */
    fun shortestPathTo(dest: GameLocation): List<PathNode> {
        if (this == dest) return emptyList()

        val visitedNodes = mutableSetOf<GameLocation>()
        val paths = mutableMapOf<GameLocation, PathNode>()
        val queue = LinkedList<Pair<GameLocation, GameLocation?>>()

        queue.add(this to null)
        while (queue.isNotEmpty()) {
            val (currentNode, parent) = queue.poll()

            // Add unvisited nodes presented by the current nodes links to the queue
            currentNode.links.mapNotNull { locations[it.dest] }.filterNot {
                visitedNodes.contains(it)
            }.map { it to currentNode }.let { queue.addAll(it) }

            // Add current node to path if a link exists
            parent?.links?.find { it.dest == currentNode.id }?.let {
                if (!paths.containsKey(currentNode)) {
                    paths[currentNode] = PathNode(parent, currentNode, it)
                }
            }

            visitedNodes.add(currentNode)
            if (currentNode == dest) {
                val list = mutableListOf<PathNode>()
                var loc = paths[currentNode]
                while (loc != null) {
                    list.add(loc)
                    loc = paths[loc.source]
                }
                return list.reversed()
            }
        }
        throw PathFindingException(this, dest)
    }
}
