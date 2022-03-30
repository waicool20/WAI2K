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

package com.waicool20.wai2k

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox
import org.slf4j.LoggerFactory
import tornadofx.*

fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler(WAI2KExceptionHandler())
    Application.launch(Wai2K::class.java, *args)
}

/**
 * Based off: https://github.com/edvin/tornadofx/blob/master/src/main/java/tornadofx/ErrorHandler.kt
 * Fixes missing stack trace in text area
 */
class WAI2KExceptionHandler : Thread.UncaughtExceptionHandler {
    val logger = LoggerFactory.getLogger("ErrorHandler")
    val ignoreStrings = listOf(
        // Infinite window spawning workaround
        "com.sun.scenario.animation.shared.PulseReceiver.timePulse"
    )

    class ErrorEvent(val thread: Thread, val error: Throwable) {
        internal var consumed = false
        fun consume() {
            consumed = true
        }
    }

    override fun uncaughtException(t: Thread, error: Throwable) {
        if (ignoreStrings.any { error.localizedMessage.contains(it) }) {
            error.printStackTrace()
            return
        }
        logger.error("Uncaught error", error)
        if (isCycle(error)) {
            logger.info("Detected cycle handling error, aborting.", error)
        } else {
            val event = ErrorEvent(t, error)
            if (!event.consumed) {
                event.consume()
                Platform.runLater { showErrorDialog(error) }
            }
        }
    }

    private fun isCycle(error: Throwable) = error.stackTrace.any {
        it.className.startsWith("${javaClass.name}\$uncaughtException$")
    }

    private fun showErrorDialog(error: Throwable) {
        val cause = Label(if (error.cause != null) error.cause?.message else "").apply {
            style = "-fx-font-weight: bold"
        }

        val textarea = TextArea().apply {
            prefRowCount = 20
            prefColumnCount = 50
            text = error.stackTraceToString()
        }

        Alert(Alert.AlertType.ERROR).apply {
            title = error.message ?: "An error occured"
            isResizable = true
            headerText =
                if (error.stackTrace.isNullOrEmpty()) "Error" else "Error in ${error.stackTrace[0]}"
            dialogPane.content = VBox().apply {
                add(cause)
                add(textarea)
            }
            showAndWait()
        }
    }
}