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

import se.vidstige.jadb.JadbDevice
import se.vidstige.jadb.JadbException
import java.io.InputStream

private var useShell = false

/**
 * Executes a given command on the device, falls back to shell if device gives a
 * closed error.
 *
 * @param command The command to execute
 * @param args Arguments for the command
 *
 * @return List of String containing each line output of the command
 */
fun JadbDevice.executeAndReadLines(command: String, vararg args: String): List<String> {
    return try {
        if (useShell) {
            executeShellAndReadLines(command, *args)
        } else {
            execute(command, *args).bufferedReader().readLines().map(String::trim)
        }
    } catch (e: JadbException) {
        if (e.localizedMessage.contains("closed")) {
            useShell = true
            executeShellAndReadLines(command, *args)
        } else throw e
    }
}

/**
 * Executes a given command on the device using shell
 *
 * @param command The command to execute
 * @param args Arguments for the command
 *
 * @return List of String containing each line output of the command
 */
fun JadbDevice.executeShellAndReadLines(command: String, vararg args: String) =
        executeShell(command, *args).bufferedReader().readLines().map(String::trim)

/**
 * Executes a given command on the device, falls back to shell if device gives a
 * closed error.
 *
 * @param command The command to execute
 * @param args Arguments for the command
 *
 * @return String containing output of the command
 */
fun JadbDevice.executeAndReadText(command: String, vararg args: String): String {
    return try {
        if (useShell) {
            executeShellAndReadText(command, *args)
        } else {
            execute(command, *args).bufferedReader().readText().trim()
        }
    } catch (e: JadbException) {
        if (e.localizedMessage.contains("closed")) {
            useShell = true
            executeShellAndReadText(command, *args)
        } else throw e
    }
}

/**
 * Executes a given command on the device using shell
 *
 * @param command The command to execute
 * @param args Arguments for the command
 *
 * @return String containing output of the command
 */
fun JadbDevice.executeShellAndReadText(command: String, vararg args: String) =
        executeShell(command, *args).bufferedReader().readText().trim()

/**
 * Just like execute, but falls back to executeShell if it fails
 *
 * @param command The command to execute
 * @param args Arguments for the command
 *
 * @return InputStream of the command
 */
fun JadbDevice.executeOrShell(command: String, vararg args: String): InputStream {
    return try {
        if (useShell) executeShell(command, *args) else execute(command, *args)
    } catch (e: JadbException) {
        if (e.localizedMessage.contains("closed")) {
            useShell = true
            executeShell(command, *args)
        } else throw e
    }
}