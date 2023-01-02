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

package com.waicool20.wai2k.scripting

import com.waicool20.cvauto.android.ADB
import com.waicool20.cvauto.android.AndroidRegion
import com.waicool20.cvauto.core.template.FileTemplate
import com.waicool20.wai2k.Wai2k
import com.waicool20.wai2k.util.Ocr
import com.waicool20.wai2k.util.loggerFor
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import net.sourceforge.tess4j.ITesseract
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

object StandaloneScriptRunner {

    private val executor = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val logger = loggerFor<StandaloneScriptRunner>()
    fun eval(path: Path): Job? {
        val job = when (val result = evalInternal(path)) {
            is ResultWithDiagnostics.Failure -> {
                result.reports.forEach {
                    logger.warn(it.toString())
                }
                null
            }
            is ResultWithDiagnostics.Success -> {
                when (val returnValue = result.value.returnValue) {
                    is ResultValue.Value -> returnValue.value as Job
                    else -> null
                }
            }
        }
        job?.invokeOnCompletion {
            logger.info("Finished running standalone script: $path")
        }
        return job
    }

    private fun evalInternal(path: Path): ResultWithDiagnostics<EvaluationResult> {
        val device =
            ADB.getDevice(Wai2k.config.lastDeviceSerial) ?: return ResultWithDiagnostics.Failure()
        val scriptLogger = LoggerFactory.getLogger(path.fileName.toString())
        val ocr = Ocr.forConfig(Wai2k.config)
        val scope = CoroutineScope(executor + CoroutineName("Standalone script: ${path.fileName}"))

        FileTemplate.checkPaths.clear()
        FileTemplate.checkPaths.add(path.parent)

        val compilationConfiguration =
            createJvmCompilationConfigurationFromTemplate<StandaloneScript> {
                jvm {
                    dependenciesFromCurrentContext(wholeClasspath = true)
                }
                providedProperties(
                    "region" to AndroidRegion::class,
                    "logger" to Logger::class,
                    "scope" to CoroutineScope::class,
                    "ocr" to ITesseract::class
                )
            }
        val evaluationConfiguration =
            createJvmEvaluationConfigurationFromTemplate<StandaloneScript> {
                providedProperties(
                    "region" to device.screens[0],
                    "logger" to scriptLogger,
                    "scope" to scope,
                    "ocr" to ocr
                )
            }
        logger.info("Running standalone script: $path")
        return BasicJvmScriptingHost().eval(
            path.toFile().toScriptSource(),
            compilationConfiguration,
            evaluationConfiguration
        )
    }
}
