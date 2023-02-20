/*
 * GPLv3 License
 *
 *  Copyright (c) waicool20
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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.waicool20.wai2k.events.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class YuuBot(var apiKey: String = "") {
    private val logger = loggerFor<YuuBot>()
    private val endpoint
        get() = System.getenv("WAI2K_ENDPOINT")
            .takeUnless { it.isNullOrEmpty() }?.plus("/api/wai2k")
            ?: "https://yuu.waicool20.com/api/wai2k"

    private val JSON_MEDIA_TYPE = "application/json".toMediaTypeOrNull()

    enum class ApiKeyStatus { VALID, INVALID, UNKNOWN }

    fun postEvent(event: ScriptEvent, onComplete: () -> Unit = {}) {
        if (apiKey.isEmpty() || apiKey.equals("off", true)) {
            logger.warn("API key is empty, YuuBot reporting is disabled.")
            return
        }
        val node = jacksonObjectMapper().createObjectNode().apply {
            put("session_id", event.sessionId)
            put("elapsed_time", event.elapsedTime)
            put("instant", event.instant.toEpochMilli())
        }

        val eventName: String

        when (event) {
            is CoalitionEnergySpentEvent -> {
                eventName = "coalition_energy_spent"
                node.put("type", "${event.type}")
                node.put("count", event.count)
            }
            is CombatReportWriteEvent -> {
                eventName = "combat_report_write"
                node.put("type", "${event.type}")
                node.put("count", event.count)
            }
            is DollDisassemblyEvent -> {
                eventName = "doll_disassembly"
                node.put("count", event.count)
            }
            is DollDropEvent -> {
                eventName = "doll_drop"
                node.put("doll", event.doll)
                node.put("map", event.map)
            }
            is DollEnhancementEvent -> {
                eventName = "doll_enhancement"
                node.put("count", event.count)
            }
            is EquipDisassemblyEvent -> {
                eventName = "equip_disassembly"
                node.put("count", event.count)
            }
            is HeartBeatEvent -> {
                eventName = "heartbeat"
            }
            is GameRestartEvent -> {
                eventName = "game_restart"
                node.put("reason", event.reason)
                node.put("map", event.map)
            }
            is LogisticsSupportReceivedEvent -> {
                eventName = "logistics_received"
            }
            is LogisticsSupportSentEvent -> {
                eventName = "logistics_sent"
            }
            is RepairEvent -> {
                eventName = "repair"
                node.put("count", event.count)
                node.put("map", event.map)
            }
            is ScriptPauseEvent -> {
                eventName = "script_pause"
            }
            is ScriptStartEvent -> {
                eventName = "script_start"
                node.put("profile_name", event.profileName)
            }
            is ScriptStopEvent -> {
                eventName = "script_stop"
                node.put("reason", event.reason)
            }
            is ScriptUnpauseEvent -> {
                eventName = "script_unpause"
            }
            is SimEnergySpentEvent -> {
                eventName = "sim_energy_spent"
                node.put("type", event.type)
                node.put("level", "${event.level}")
                node.put("count", event.count)
            }
            is SortieDoneEvent -> {
                eventName = "sortie_done"
                node.put("map", event.map)
                node.put("dragger1", event.draggers.getOrNull(0)?.id)
                node.put("dragger2", event.draggers.getOrNull(1)?.id)
            }
            is ScriptStatsUpdateEvent -> return
        }

        val request = Request.Builder()
            .url("$endpoint/event/$eventName")
            .header("Authorization", "Bearer $apiKey")
            .post(node.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                when (val code = response.code) {
                    200 -> {
                        logger.info("Posted ${event::class.simpleName} to YuuBot, response was: $code")
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

    fun postMessage(title: String, body: String, onComplete: () -> Unit = {}) {
        if (apiKey.isEmpty() || apiKey.equals("off", true)) {
            logger.warn("API key is empty, YuuBot reporting is disabled.")
            return
        }
        logger.info("Posting message to YuuBot...")

        val jsonBody = jacksonObjectMapper()
            .writeValueAsString(
                mapOf(
                    "title" to title,
                    "body" to body
                )
            ).toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$endpoint/message")
            .header("Authorization", "Bearer $apiKey")
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

    fun testApiKey(onComplete: (ApiKeyStatus) -> Unit = {}) {
        if (apiKey.isEmpty() || apiKey.equals("off", true)) {
            logger.warn("API key is empty, YuuBot reporting is disabled.")
            onComplete(ApiKeyStatus.INVALID)
            return
        }
        val request = Request.Builder()
            .url("$endpoint/")
            .header("Authorization", "Bearer $apiKey")
            .build()

        logger.info("Testing API key: $apiKey")
        OkHttpClient().newCall(request).enqueue(object : Callback {
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
