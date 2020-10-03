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

package com.waicool20.wai2k.launcher

import okhttp3.*
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.xml.bind.DatatypeConverter
import kotlin.concurrent.thread
import kotlin.system.exitProcess

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

object Main {
    private val client = OkHttpClient()
    private val url = "https://wai2k.waicool20.com/files"
    private val appPath = Paths.get(System.getProperty("user.home")).resolve(".wai2k").toAbsolutePath()
    private val libPath = appPath.resolve("libs")

    val mainFiles = listOf("WAI2K.jar", "assets.zip", "models.zip")

    val label = JLabel().apply {
        text = "Launching WAI2K"
    }

    val frame = JFrame("WAI2K Launcher").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        add(
            JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                add(label)
            }
        )
        size = Dimension(500, 75)
        setLocationRelativeTo(null)
        isResizable = false
        isVisible = true
    }

    init {
        if (Files.notExists(appPath)) Files.createDirectories(appPath)
        if (Files.notExists(libPath)) Files.createDirectories(libPath)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (!args.contains("--skip-checks")) {
            try {
                checkLauncherUpdate()
                mainFiles.forEach(::checkFile)
                checkDependencies()
            } catch (e: Exception) {
                println("Exception during update check ${e.message}")
                // Just try to launch wai2k anyways if anything unexpected happens ¯\_(ツ)_/¯
            }
        }
        launchWai2K(args)
    }

    private fun checkFile(file: String) {
        label.text = "Checking file $file"
        val path = appPath.resolve(file)
        try {
            if (Files.exists(path)) {
                val sum = client.newCall(Request.Builder().url("$url/$file.md5").build())
                    .execute().use { it.body!!.string() }
                if (sum.equals(calcCheckSum(path), true)) return
            }

            client.newCall(Request.Builder().url("$url/$file").build()).execute().use {
                println("[DOWNLOAD] $file")
                val input = it.body!!.byteStream()
                val output = Files.newOutputStream(path)
                input.copyTo(output)
                input.close()
                output.close()
                println("[OK] $file")
            }

            if ("$path".endsWith(".zip")) unzip(path, appPath.resolve("wai2k"))
        } catch (e: Exception) {
            if (Files.exists(path)) {
                println("Skipping $file update check due to exception: ${e.message}")
                return
            } else {
                halt("Could not grab initial copy of $file")
            }
        }
    }

    private fun checkLauncherUpdate() {
        // Skip update check if running from code
        if (!"${Main.javaClass.getResource(Main.javaClass.simpleName + ".class")}".startsWith("jar")) return
        val jarPath = Paths.get(Main::class.java.protectionDomain.codeSource.location.toURI())

        try {
            val md5Url = "https://github.com/waicool20/WAI2K/releases/download/Latest/WAI2K-Launcher.jar.md5"
            val request = Request.Builder().url(md5Url).build()
            val sum = client.newCall(request).execute().use { it.body!!.string() }
            if (sum.equals(calcCheckSum(jarPath), true)) return

            val uri = URI("https://github.com/waicool20/WAI2K/releases/tag/Latest")
            thread {
                try {
                    Desktop.getDesktop().browse(uri)
                } catch (e: Exception) {
                    if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                        ProcessBuilder("xdg-open", "$uri").start()
                    } else {
                        throw e
                    }
                }
            }
            halt("Launcher update available, please download it and try again")
        } catch (e: Exception) {
            println("Skipping launcher update check due to exception: ${e.message}")
        }
    }

    private fun checkDependencies() {
        val depPath = appPath.resolve("dependencies.txt")
        val text = if (Files.exists(depPath)) {
            Files.readAllLines(depPath)
        } else {
            try {
                val request = Request.Builder().url("$url/dependencies.txt").build()
                val text = client.newCall(request).execute().use { it.body!!.string() }
                Files.write(depPath, text.toByteArray())
                text.lines()
            } catch (e: Exception) {
                halt("Could not retrieve dependency list: ${e.message}")
            }
        }

        val repos = mutableListOf<String>()
        val depsString = mutableListOf<String>()

        var isRepo = false

        for (line in text) {
            when {
                line.startsWith("Repositories:") -> isRepo = true
                line.startsWith("Dependencies") -> isRepo = false
                line.startsWith("- ") -> {
                    val entry = line.drop(2)
                    if (isRepo) {
                        repos.add(entry)
                    } else {
                        depsString.add(entry)
                    }
                }
            }
        }

        for (i in 0 until 10) {
            val deps = depsString
                .map { it.split(":") }
                .filterNot { (_, name, version) ->
                    verifyCheckSum(libPath.resolve("$name-$version.jar"))
                }

            val depsTotal = deps.size * 2
            if (depsTotal == 0) {
                return
            } else {
                if (i > 0) TimeUnit.SECONDS.sleep(1)
            }
            val latch = CountDownLatch(depsTotal)

            fun downloadFile(path: Path, response: Response) {
                println("[DOWNLOAD] $path")
                val input = response.body!!.byteStream()
                val output = Files.newOutputStream(path)
                input.copyTo(output)
                input.close()
                output.close()
                println("[OK] $path")
                latch.countDown()
                label.text = "Downloading libraries: ${depsTotal - latch.count}/${depsTotal}"
            }

            label.text = "Downloading libraries: ${depsTotal - latch.count}/${depsTotal}"
            for ((grp, name, version) in deps) {
                val group = grp.replace(".", "/")
                for (repo in repos) {
                    val url = if (repo.endsWith("/")) repo else "$repo/"
                    val filename = "$name-$version.jar"
                    val path = libPath.resolve(filename)
                    if (verifyCheckSum(path)) {
                        println("[OK] $path")
                        break
                    }

                    client.newCall(Request.Builder().url("$url$group/$name/$version/$filename").build())
                        .enqueue(object : Callback {
                            override fun onResponse(call: Call, response: Response) {
                                if (response.code == 200) downloadFile(path, response)
                            }

                            override fun onFailure(call: Call, e: IOException) {
                                // Do Nothing
                            }
                        })
                    client.newCall(Request.Builder().url("$url$group/$name/$version/$filename.md5").build())
                        .enqueue(object : Callback {
                            override fun onResponse(call: Call, response: Response) {
                                if (response.code == 200) downloadFile(libPath.resolve("$filename.md5"), response)
                            }

                            override fun onFailure(call: Call, e: IOException) {
                                // Do Nothing
                            }
                        })
                    break
                }
            }
            latch.await()
        }

        halt("Failed to download libraries, try deleting .wai2k/libs and try again")
    }

    private fun halt(msg: String): Nothing {
        label.text = msg
        while (true) TimeUnit.SECONDS.sleep(1)
    }

    private fun verifyCheckSum(file: Path): Boolean {
        if (Files.notExists(file)) return false
        val md5sumFile = Paths.get("$file.md5")
        if (Files.notExists(md5sumFile)) return false
        return calcCheckSum(file).equals(Files.readAllBytes(md5sumFile)
            .toString(Charsets.UTF_8).take(32), ignoreCase = true)
    }

    private fun calcCheckSum(file: Path): String {
        return MessageDigest.getInstance("MD5")
            .digest(Files.readAllBytes(file))
            .let { DatatypeConverter.printHexBinary(it) }
    }

    private fun unzip(file: Path, destination: Path) {
        label.text = "Unpacking ${file.fileName}"
        val zis = ZipInputStream(Files.newInputStream(file))
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val outputFile = destination.resolve(entry.name)
                Files.createDirectories(outputFile.parent)
                val output = Files.newOutputStream(outputFile)
                zis.copyTo(output)
                output.close()
            }
            entry = zis.nextEntry
        }
        zis.closeEntry()
        zis.close()
    }

    private fun launchWai2K(args: Array<String>) {
        frame.isVisible = false
        frame.dispose()
        val classpath = if (System.getProperty("os.name").contains("win", true)) {
            "$libPath\\*;$appPath\\WAI2K.jar"
        } else {
            "$libPath/*:$appPath/WAI2K.jar"
        }
        println("Launching WAI2K")
        println("Classpath: $classpath")
        println("Args: ${args.joinToString()}")
        val process = ProcessBuilder("java", "-cp",
            classpath,
            "com.waicool20.wai2k.LauncherKt",
            *args
        ).inheritIO().start()
        process.waitFor()
        exitProcess(process.exitValue())
    }
}