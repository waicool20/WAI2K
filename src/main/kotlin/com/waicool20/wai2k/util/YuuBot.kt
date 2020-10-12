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

import com.fasterxml.jackson.databind.annotation.JsonAppend
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.waicool20.wai2k.config.Wai2KProfile
import com.waicool20.wai2k.script.ScriptStats
import com.waicool20.waicoolutils.logging.loggerFor
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant


object YuuBot {
    private val logger = loggerFor<YuuBot>()
    private val endpoint
        get() = System.getenv("WAI2K_ENDPOINT").takeUnless { it.isNullOrEmpty() }
            ?: "https://yuu.waicool20.com/api/wai2k/user"

    private val JSON_MEDIA_TYPE = "application/json".toMediaTypeOrNull()

    enum class ApiKeyStatus { VALID, INVALID, UNKNOWN }

    @JsonAppend(attrs = [
        JsonAppend.Attr(value = "profileName"),
        JsonAppend.Attr(value = "map"),
        JsonAppend.Attr(value = "dragger1"),
        JsonAppend.Attr(value = "dragger2")
    ])
    private class ScriptStatsMixin

    fun postStats(
        apiKey: String,
        startTime: Instant,
        profile: Wai2KProfile,
        stats: ScriptStats,
        onComplete: () -> Unit = {}
    ) {
        if (apiKey.isEmpty()) {
            logger.warn("API key is empty, YuuBot reporting is disabled.")
            return
        }
        logger.info("Posting stats to YuuBot...")

        val attr = mutableMapOf("profileName" to profile.name)
        if (profile.combat.enabled) {
            attr["map"] = profile.combat.map
            attr["dragger1"] = profile.combat.draggers[0].id
            attr["dragger2"] = profile.combat.draggers[1].id
        }

        val body = jacksonObjectMapper()
            .addMixIn(ScriptStats::class.java, ScriptStatsMixin::class.java)
            .writer()
            .withAttributes(attr)
            .writeValueAsString(stats)
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$endpoint/$apiKey/stats/${startTime.toEpochMilli()}")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                when (val code = response.code) {
                    200 -> {
                        logger.info("Posted stats to YuuBot, response was: $code")
                    }
                    else -> {
                        logger.warn("Failed to post stats to YuuBot, response was: $code")
                    }
                }
                onComplete()
            }

            override fun onFailure(call: Call, e: IOException) {
                logger.warn("Failed to post stats to YuuBot, maybe your internet is down?")
            }
        })
    }

    fun postMessage(apiKey: String, title: String, body: String, onComplete: () -> Unit = {}) {
        if (apiKey.isEmpty()) {
            logger.warn("API key is empty, YuuBot reporting is disabled.")
            return
        }
        logger.info("Posting message to YuuBot...")

        val jsonBody = jacksonObjectMapper()
            .writeValueAsString(mapOf(
                "title" to title,
                "body" to body
            )).toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$endpoint/$apiKey/message/")
            .post(jsonBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                when (val code = response.code) {
                    200 -> {
                        logger.info("Posted message to YuuBot, response was: $code")
                    }
                    else -> {
                        logger.warn("Failed to post message to YuuBot, response was: $code")
                    }
                }
                onComplete()
            }

            override fun onFailure(call: Call, e: IOException) {
                logger.warn("Failed to post message to YuuBot, maybe your internet is down?")
            }
        })
    }

    fun testApiKey(apiKey: String, onComplete: (ApiKeyStatus) -> Unit) {
        if (apiKey.isEmpty()) {
            logger.warn("API key is empty, YuuBot reporting is disabled.")
            onComplete(ApiKeyStatus.INVALID)
            return
        }
        logger.info("Testing API key: $apiKey")
        OkHttpClient().newCall(Request.Builder().url("$endpoint/$apiKey").build()).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                when (val code = response.code) {
                    200 -> {
                        onComplete(ApiKeyStatus.VALID)
                        logger.info("API key was found valid, response was: $code")
                    }
                    else -> {
                        onComplete(ApiKeyStatus.INVALID)
                        logger.warn("API key was found invalid, response was: $code")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                logger.warn("Could not check if API key is valid, maybe your internet is down?")
                onComplete(ApiKeyStatus.UNKNOWN)
            }
        })
    }
}