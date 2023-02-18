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

package com.waicool20.wai2k.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import java.time.Instant

object EventBus {
    abstract class Event(val instant: Instant = Instant.now())

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 20)
    val events = _events.asSharedFlow()

    fun tryPublish(event: Event): Boolean {
        return _events.tryEmit(event)
    }

    suspend fun publish(event: Event) {
        _events.emit(event)
    }

    inline fun <reified T> subscribe(): Flow<T> {
        return events.filterIsInstance()
    }
}
