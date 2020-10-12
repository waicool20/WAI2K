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

package com.waicool20.wai2k.util.ai

import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.modality.cv.output.Rectangle

sealed class GFLObject {
    abstract val probability: Double
    abstract val bbox: Rectangle

    open class Node(override val probability: Double, override val bbox: Rectangle) : GFLObject()
    class CommandPost(override val probability: Double, override val bbox: Rectangle) : Node(probability, bbox)
    class Heliport(override val probability: Double, override val bbox: Rectangle) : Node(probability, bbox)
    class SupplyCrate(override val probability: Double, override val bbox: Rectangle) : Node(probability, bbox)
    class Radar(override val probability: Double, override val bbox: Rectangle) : Node(probability, bbox)

    abstract class Unit : GFLObject()

    open class Enemy(override val probability: Double, override val bbox: Rectangle) : Unit()
    class SangvisFerri(override val probability: Double, override val bbox: Rectangle) : Enemy(probability, bbox)
    class Military(override val probability: Double, override val bbox: Rectangle) : Enemy(probability, bbox)
    class Paradeus(override val probability: Double, override val bbox: Rectangle) : Enemy(probability, bbox)

    open class Friendly(override val probability: Double, override val bbox: Rectangle) : Unit()

    companion object {
        /**
         * All possible values of [GFLObject], must be declared in the order the model was created with
         */
        val values = listOf(
            Node::class,
            CommandPost::class,
            Heliport::class,
            Enemy::class,
            SangvisFerri::class,
            Military::class,
            Radar::class,
            Paradeus::class,
            SupplyCrate::class,
            Friendly::class
        )
    }

    override fun toString(): String {
        return this::class.simpleName?.replace(Regex("(.)([A-Z])"), "$1 $2") ?: "Unknown"
    }
}

fun List<GFLObject>.toDetectedObjects(): DetectedObjects {
    return DetectedObjects(
        map { it.toString() },
        map { it.probability },
        map { it.bbox }
    )
}