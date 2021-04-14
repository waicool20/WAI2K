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

package com.waicool20.wai2k.util

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * Implements a simple watchdog timer, which operates with 1s precision
 *
 * @param initialTime Initial time in ms
 * @param onExpire Callback for when this timer expires
 */
class WatchDogTimer(private val initialTime: Long, private val onExpire: () -> Unit = {}) {
    private val ticks = AtomicLong(initialTime)
    private val stopFlag = AtomicBoolean(false)
    private val expiredFlag = AtomicBoolean(false)

    /**
     * Starts the timer
     */
    fun start() {
        expiredFlag.set(false)
        thread(name = "WatchDogTimer") {
            while (ticks.get() > 0) {
                Thread.sleep(1000)
                println(ticks.addAndGet(-1000))
                if (stopFlag.getAndSet(false)) {
                    reset()
                    return@thread
                }
            }
            expiredFlag.set(true)
            onExpire()
        }
    }

    /**
     * Stops the timer
     */
    fun stop() {
        stopFlag.set(true)
    }

    /**
     * Resets the timer to [initialTime] but does not stop it if it is already running
     */
    fun reset() {
        ticks.set(initialTime)
    }

    /**
     * Returns the remaining number of milliseconds before the timer expires
     */
    fun remainingTime() = ticks

    /**
     * Returns true if this timer has expired
     */
    fun hasExpired() = expiredFlag.get()

    /**
     * Use this to add additional time
     */
    fun addTime(t: Long, unit: TimeUnit) {
        ticks.getAndAdd(unit.toMillis(t))
    }
}