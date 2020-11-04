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

package com.waicool20.wai2k.script

import com.waicool20.wai2k.game.GameLocation
import com.waicool20.wai2k.game.LocationId
import com.waicool20.wai2k.script.modules.combat.MapNode

open class ScriptException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class ScriptTimeOutException(reason: String, cause: Throwable? = null) : ScriptException("Timed out: $reason", cause)
class InvalidDestinationException(location: LocationId) : ScriptException("Invalid destination: $location")
class UnknownLocationException : ScriptException("Current location could not be identified")
class PathFindingException(from: GameLocation, to: GameLocation) : ScriptException("No path found from $from to $to")
class InvalidMapNameException(mapName: String) : ScriptException("Invalid map name: $mapName")
class NodeNotFoundException(node: MapNode) : ScriptException("Could not find node: $node")
class ChapterClickFailedException(chapter: Int) : ScriptException("Failed to find and click chapter $chapter")
class InvalidLocationsJsonFileException : ScriptException("Bad or incomplete locations.json file")
class UnsupportedMapException(mapName: String) : ScriptException("Unsupported map: $mapName")
class ReplacementDollNotFoundException : ScriptException("Could not find replacement dragging doll")
class InvalidDollException(id: String) : ScriptException("Invalid doll: $id")
class RepairUpdateException: ScriptException("Failed to update repair status, bad OCR?")