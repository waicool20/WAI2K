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

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.concurrent.thread
import kotlin.streams.toList
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
    enum class Hash(val length: Int) {
        MD5(32),
        SHA1(40)
    }

    private val client = OkHttpClient()
    private val url = "https://wai2k.waicool20.com/files"
    private val appPath = Paths.get(System.getProperty("user.home")).resolve(".wai2k").toAbsolutePath()
    private val libPath = appPath.resolve("libs")
    private val depPath = appPath.resolve("dependencies.txt")

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
        checkJavaVersion()
        if (!args.contains("--skip-checks")) {
            try {
                if (!args.contains("--skip-launcher-check")) {
                    checkLauncherUpdate()
                }
                if (!args.contains("--skip-main-check")) {
                    mainFiles.forEach(::checkFile)
                }
                if (!args.contains("--skip-dependencies-check")) {
                    checkDependencies(args.contains("--use-local-dep-list"))
                }
            } catch (e: Exception) {
                println("Exception during update check")
                e.printStackTrace()
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
                val chksum0 = grabWebString("$url/$file.md5")
                val chksum1 = calcCheckSum(path, Hash.MD5)
                if (chksum0.equals(chksum1, true)) return
            }

            client.newCall(Request.Builder().url("$url/$file").build()).execute().use {
                if (!it.isSuccessful) error("Bad server response: ${it.code}")
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
                println("Skipping $file update check due to exception")
                e.printStackTrace()
                return
            } else {
                halt("Could not grab initial copy of $file")
            }
        }
    }

    private fun checkJavaVersion() {
        val v = System.getProperty("java.version")
        if (v.takeWhile { it != '.' }.toInt() < 11) {
            browseLink("https://adoptopenjdk.net/")
            halt("WAI2K has updated to Java 11+, you have version $v")
        }
        println("Java OK: $v")
    }

    private fun checkLauncherUpdate() {
        // Skip update check if running from code
        if (!"${Main.javaClass.getResource(Main.javaClass.simpleName + ".class")}".startsWith("jar")) return
        val jarPath = Paths.get(Main::class.java.protectionDomain.codeSource.location.toURI())

        try {
            val chksum0 = grabWebString("https://github.com/waicool20/WAI2K/releases/download/Latest/WAI2K-Launcher.jar.md5")
            val chksum1 = calcCheckSum(jarPath, Hash.MD5)
            if (chksum0.equals(chksum1, true)) return
            browseLink("https://github.com/waicool20/WAI2K/releases/tag/Latest")
            halt("Launcher update available, please download it and try again")
        } catch (e: Exception) {
            println("Skipping launcher update check due to exception")
            e.printStackTrace()
        }
    }

    private fun checkDependencies(useLocalDepList: Boolean) {
        label.text = "Checking dependencies..."

        // Download libs to local
        System.setProperty("maven.repo.local", libPath.toString())

        val text = try {
            if (useLocalDepList && Files.exists(depPath)) {
                Files.readAllLines(depPath)
            } else {
                val text = grabWebString("$url/dependencies.txt")
                if (Files.exists(depPath)) {
                    val oldText = Files.readAllBytes(depPath).toString(Charsets.UTF_8)
                    if (oldText != text) {
                        println("Dependencies changed, deleting old ones...")
                        Files.walk(libPath).sorted(Comparator.reverseOrder())
                            .forEach { Files.delete(it) }
                    }
                }
                Files.write(depPath, text.toByteArray())
                text.lines()
            }
        } catch (e: Exception) {
            if (Files.exists(depPath)) {
                println("Could not retrieve new dependencies, using old one instead")
                e.printStackTrace()
                Files.readAllLines(depPath)
            } else {
                halt("Could not retrieve dependency list: ${e.message}")
            }
        }

        var maven = Maven.configureResolver()
            // Disabled for easier debugging since it resolves jars from launcher classpath
            .withClassPathResolution(false)
        val dependencies = mutableListOf<String>()

        var isRepo = false

        for ((i, line) in text.withIndex()) {
            when {
                line.startsWith("Repositories:") -> isRepo = true
                line.startsWith("Dependencies") -> isRepo = false
                line.startsWith("- ") -> {
                    val entry = line.drop(2)
                    if (isRepo) {
                        println("Register repository: $line")
                        maven = maven.withRemoteRepo(i.toString(), entry, "default")
                    } else {
                        dependencies.add(entry)
                    }
                }
            }
        }

        dependencies.forEach { d ->
            label.text = "Checking dependency: $d"
            println("Resolving dependency $d")
            var paths: List<Path>
            while (true) {
                paths = maven.resolve(d).withTransitivity().asFile().map { it.toPath() }

                // Skip sha1 check on snapshots
                if (d.contains("snapshot", true)) break
                val failedChecks = paths.filterNot { verifyCheckSum(it, Hash.SHA1) }
                if (failedChecks.isEmpty()) break
                failedChecks.forEach {
                    Files.deleteIfExists(it)
                    Files.deleteIfExists(Paths.get("${it}.sha1"))
                }
            }
            println("Found dependency @ ${paths.firstOrNull()}")
        }
    }

    private fun browseLink(link: String) {
        val uri = URI(link)
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
    }

    private fun grabWebString(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use {
            if (!it.isSuccessful) error("Bad server response: ${it.code}")
            it.body!!.string()
        }
    }

    private fun halt(msg: String): Nothing {
        label.text = msg
        while (true) TimeUnit.SECONDS.sleep(1)
    }

    private fun verifyCheckSum(file: Path, hash: Hash): Boolean {
        if (Files.notExists(file)) return false
        val sumPath = when (hash) {
            Hash.MD5 -> Paths.get("$file.md5")
            Hash.SHA1 -> Paths.get("$file.sha1")
        }
        if (Files.notExists(sumPath)) return false
        val chksum0 = Files.readAllBytes(sumPath).toString(Charsets.UTF_8).take(hash.length)
        val chksum1 = calcCheckSum(file, hash)
        return chksum0.equals(chksum1, true)
    }

    private fun calcCheckSum(file: Path, hash: Hash): String {
        val digest = when (hash) {
            Hash.MD5 -> MessageDigest.getInstance("MD5")
            Hash.SHA1 -> MessageDigest.getInstance("SHA-1")
        }
        return digest.digest(Files.readAllBytes(file))
            .joinToString("") { String.format("%02x", it) }
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

        val jars = Files.walk(libPath)
            .filter { Files.isRegularFile(it) && "$it".endsWith(".jar") }
            .toList()
            // Prioritize higher versioned libraries
            .sortedDescending()

        val classpath = if (System.getProperty("os.name").contains("win", true)) {
            jars.joinToString(";", postfix = ";") + "$appPath\\WAI2K.jar"
        } else {
            jars.joinToString(":", postfix = ":") + "$appPath/WAI2K.jar"
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