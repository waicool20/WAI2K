package com.waicool20.wai2k.android.input

import com.waicool20.wai2k.android.enums.InputEvent

data class AndroidInput(
        val devFile: String,
        val name: String,
        val specs: Map<InputEvent, TouchSpec>
) {
    companion object {
        fun parse(deviceInfo: String): AndroidInput {
            val lines = deviceInfo.lines()
            val devFile = lines[0].takeLastWhile { it != ' ' }
            val name = lines[1].dropWhile { it != '"' }.removeSurrounding("\"")
            val specs = lines.subList(2, lines.lastIndex)
                    .mapNotNull { TouchSpec.REGEX.matchEntire(it)?.groupValues }
                    .mapNotNull {
                        InputEvent.findByCode(it[1].toLong(16))?.let { code ->
                            code to TouchSpec(it[2].toInt(), it[3].toInt(), it[4].toInt(), it[5].toInt(), it[6].toInt())
                        }
                    }.toMap()
            return AndroidInput(devFile, name, specs)
        }
    }
}

data class TouchSpec(
        val minValue: Int,
        val maxValue: Int,
        val fuzz: Int,
        val flat: Int,
        val resolution: Int
) {
    companion object {
        val REGEX = Regex(".*?(\\w{4})\\s+:\\s+value \\d+, min (\\d+), max (\\d+), fuzz (\\d+), flat (\\d+), resolution (\\d+).*?")
    }
}
