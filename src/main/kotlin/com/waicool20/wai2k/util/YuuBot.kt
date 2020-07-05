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

import com.waicool20.waicoolutils.logging.loggerFor
import okhttp3.*
import java.io.IOException


object YuuBot {
    private val logger = loggerFor<YuuBot>()
    private val endpoint
        get() = System.getenv("WAI2K_ENDPOINT").takeUnless { it.isNullOrEmpty() }
                ?: "https://yuu.waicool20.com/api/wai2k/user/"

    enum class ApiKeyStatus { VALID, INVALID, UNKNOWN }

    fun testApiKey(apiKey: String, onComplete: (ApiKeyStatus) -> Unit) {
        if (apiKey.isEmpty()) {
            logger.info("API key is empty, YuuBot reporting is disabled.")
            onComplete(ApiKeyStatus.INVALID)
            return
        }
        logger.info("Testing API key: $apiKey")
        OkHttpClient().newCall(Request.Builder().url(endpoint + apiKey).build()).enqueue(object : Callback {
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