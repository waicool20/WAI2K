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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.waicool20.cvauto.android.AndroidDevice
import com.waicool20.cvauto.core.Region
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.config.Wai2KConfig
import com.waicool20.wai2k.script.Asset
import com.waicool20.wai2k.script.InvalidLocationsJsonFileException
import com.waicool20.wai2k.script.PathFindingException
import com.waicool20.waicoolutils.logging.loggerFor
import java.util.*

/**
 * Represents a location in the game
 *
 * @param id Id of this location
 * @param isIntermediate If this is intermediate then that means this location
 * can potentially be skipped to get to a destination when traversing a path.
 */
data class GameLocation(
    val id: LocationId,
    val isIntermediate: Boolean = false,
    val matchingMode: Mode = Mode.AND
) {
    enum class Mode {
        AND, OR
    }
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
                mapper.readValue<List<GameLocation>>(file).associateBy { it.id }.also {
                    loaderLogger.info("Loaded ${it.size} location entries")
                    locations = it
                }
            } else locations
        }

        fun find(wai2KConfig: Wai2KConfig, location: LocationId): GameLocation {
            return mappings(wai2KConfig)[location] ?: throw InvalidLocationsJsonFileException()
        }
    }

    /**
     * Function to check if a location is on screen
     *
     * @return True if location is on screen
     */
    fun isInRegion(region: Region<AndroidDevice>): Boolean {
        if (landmarks.isEmpty()) return false
        return when (matchingMode) {
            Mode.AND -> landmarks.all { it.asset.getSubRegionFor(region).has(FileTemplate(it.asset.imagePath, 0.98)) }
            Mode.OR -> landmarks.any { it.asset.getSubRegionFor(region).has(FileTemplate(it.asset.imagePath, 0.98)) }
        }
    }

    /**
     * Wrapper class describing a node in a path
     *
     * @param source Source location
     * @param dest Destination location
     * @param link Link needed to traverse from source to dest
     */
    data class GameLocationLink(val source: GameLocation, val dest: GameLocation, val link: Link)

    /**
     * Finds the shortest path to some destination
     *
     * @param dest Destination
     * @return List of [GameLocationLink], null if no path solution is found
     */
    fun shortestPathTo(dest: GameLocation): List<GameLocationLink> {
        if (this == dest) return emptyList()

        val visitedNodes = mutableSetOf<GameLocation>()
        val paths = mutableMapOf<GameLocation, GameLocationLink>()
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
                    paths[currentNode] = GameLocationLink(parent, currentNode, it)
                }
            }

            visitedNodes.add(currentNode)
            if (currentNode == dest) {
                val list = mutableListOf<GameLocationLink>()
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