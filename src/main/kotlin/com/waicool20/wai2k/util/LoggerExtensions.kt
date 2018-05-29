package com.waicool20.wai2k.util

import org.slf4j.LoggerFactory

inline fun <reified T> loggerFor() = LoggerFactory.getLogger(T::class.java)
