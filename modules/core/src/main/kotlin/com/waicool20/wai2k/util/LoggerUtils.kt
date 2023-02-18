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

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Gets a logger for a class
 */
inline fun <reified T> loggerFor(): Logger = LoggerFactory.getLogger(T::class.java)

fun Logger.setLogLevel(level: String) {
    val ctx = LogManager.getContext(false) as LoggerContext
    val loggerConfig = ctx.configuration.getLoggerConfig(name)
    loggerConfig.level = Level.valueOf(level)
    ctx.updateLoggers()
}
