package com.waicool20.wai2k.util

import se.vidstige.jadb.JadbDevice

fun JadbDevice.executeAndReadLines(command: String, vararg args: String) =
        execute(command, *args).bufferedReader().readLines().map(String::trim)

fun JadbDevice.executeAndReadText(command: String, vararg args: String) =
        execute(command, *args).bufferedReader().readText().trim()
