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
import okhttp3.Protocol
import okhttp3.Request
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.concurrent.thread
import kotlin.io.path.*
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

    private var ignoreSSL = false
    private val client: OkHttpClient
        get() {
            val b = OkHttpClient().newBuilder()
                .readTimeout(20, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .protocols(listOf(Protocol.HTTP_1_1))
            return if (ignoreSSL) {
                b.hostnameVerifier { _, _ -> true }.build()
            } else {
                b.build()
            }
        }
    private val formatter get() = DecimalFormat("#.#")
    private val url = "https://wai2k.waicool20.com/files"
    private val appPath = Path(System.getProperty("user.home"), ".wai2k").absolute()
    private val libPath = appPath.resolve("libs")
    private val jarPath = run {
        // Skip update check if running from code
        if ("${Main.javaClass.getResource(Main.javaClass.simpleName + ".class")}".startsWith("jar")) {
            Main::class.java.protectionDomain.codeSource.location.toURI().toPath()
        } else null
    }

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
        appPath.createDirectories()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("Launcher path: $jarPath")
        checkJavaVersion()
        ignoreSSL = args.contains("--ignore-ssl") || jarPath?.name?.contains("IgnoreSSL") == true
        if (!args.contains("--skip-updates") && jarPath?.name?.contains("SkipUpdates") != true) {
            try {
                if (!args.contains("--skip-launcher-update")) {
                    checkLauncherUpdate()
                }
                if (!args.contains("--skip-main-update")) {
                    mainFiles.forEach(::checkFile)
                }
                if (!args.contains("--skip-libs-update")) {
                    checkLibsUpdate()
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
            if (path.exists()) {
                val chksum0 = grabWebString("$url/$file.md5")
                val chksum1 = calcCheckSum(path, Hash.MD5)
                if (chksum0.equals(chksum1, true)) return
            }

            client.newCall(Request.Builder().url("$url/$file").build()).execute().use {
                if (!it.isSuccessful) error("Bad server response: ${it.code}")
                println("[DOWNLOAD] $file")
                val body = it.body!!
                val total = body.contentLength()
                val input = body.byteStream()
                val output = path.outputStream()
                input.copyWithProgress(output) { i ->
                    label.text = "Downloading $file: $i / $total " +
                        "(${formatter.format(i / total.toDouble() * 100)} %)"
                }
                println("[OK] $file")
            }

            if (path.extension == "zip") unzip(path, appPath.resolve("wai2k"))
        } catch (e: Exception) {
            if (path.exists()) {
                println("Skipping $file update check due to exception")
                e.printStackTrace()
                return
            } else {
                halt("Could not grab initial copy of $file: $e")
            }
        }
    }

    private fun checkLibsUpdate() {
        label.text = "Checking libraries"
        val path = appPath.resolve("libs.zip")
        try {
            if (path.exists()) {
                val chksum0 = grabWebString("$url/libs.zip.md5")
                val chksum1 = calcCheckSum(path, Hash.MD5)
                if (chksum0.equals(chksum1, true)) {
                    if (libPath.notExists()) unzip(path, appPath)
                    return
                }
            }

            if (libPath.exists()) {
                Files.walk(appPath.resolve("libs")).sorted(Comparator.reverseOrder())
                    .forEach { it.deleteExisting() }
            }

            client.newCall(Request.Builder().url("$url/libs.zip").build()).execute().use {
                if (!it.isSuccessful) error("Bad server response: ${it.code}")
                println("[DOWNLOAD] libs.zip")
                val body = it.body!!
                val total = body.contentLength()
                val input = body.byteStream()
                val output = path.outputStream()
                input.copyWithProgress(output) { i ->
                    label.text = "Downloading libs.zip: $i / $total " +
                        "(${formatter.format(i / total.toDouble() * 100)} %)"
                }
                input.close()
                output.close()
                println("[OK] libs.zip")
            }

            unzip(path, appPath)
        } catch (e: Exception) {
            if (path.exists()) {
                println("Skipping libs.zip update check due to exception")
                e.printStackTrace()
                return
            } else {
                halt("Could not grab initial copy of libs.zip")
            }
        }
    }

    private fun InputStream.copyWithProgress(output: OutputStream, callback: (Long) -> Unit) {
        val buffer = ByteArray(8192)
        var bytesCopied = 0L
        var bytes = read(buffer)
        while (bytes >= 0) {
            output.write(buffer, 0, bytes)
            bytesCopied += bytes
            callback(bytesCopied)
            bytes = read(buffer)
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
        if (jarPath == null) return
        try {
            val chksum0 = grabWebString("https://wai2k.waicool20.com/files/WAI2K-Launcher.jar.md5")
            val chksum1 = calcCheckSum(jarPath, Hash.MD5)
            if (chksum0.equals(chksum1, true)) return
            browseLink("https://github.com/waicool20/WAI2K/releases/tag/Latest")
            halt("Launcher update available, please download it and try again")
        } catch (e: Exception) {
            println("Skipping launcher update check due to exception")
            e.printStackTrace()
        }
    }

    private fun browseLink(link: String) {
        val uri = URI(link)
        thread {
            try {
                Desktop.getDesktop().browse(uri)
            } catch (e: Exception) {
                if (System.getProperty("os.name").lowercase().contains("linux")) {
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
        label.text = "<html><div style=\"width:250px;\">$msg</div></html>"
        frame.pack()
        while (true) TimeUnit.SECONDS.sleep(1)
    }

    private fun calcCheckSum(file: Path, hash: Hash): String {
        val digest = when (hash) {
            Hash.MD5 -> MessageDigest.getInstance("MD5")
            Hash.SHA1 -> MessageDigest.getInstance("SHA-1")
        }
        val buffer = ByteArray(1024)
        file.inputStream().use { inputStream ->
            var read = 0
            while (true) {
                read = inputStream.read(buffer, 0, buffer.size)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { String.format("%02x", it) }
    }

    private fun unzip(file: Path, destination: Path) {
        label.text = "Unpacking ${file.fileName}"
        val zis = ZipInputStream(file.inputStream())
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val outputFile = destination.resolve(entry.name)
                outputFile.parent.createDirectories()
                val output = outputFile.outputStream()
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
            .filter { it.isRegularFile() && it.extension == "jar" }
            .toList()
            .sortedDescending()

        val classpath = if (System.getProperty("os.name").contains("win", true)) {
            jars.joinToString(";", postfix = ";") + "$appPath\\WAI2K.jar"
        } else {
            jars.joinToString(":", postfix = ":") + "$appPath/WAI2K.jar"
        }

        println("Launching WAI2K")
        println("Classpath: $classpath")
        println("Args: ${args.joinToString()}")
        val process = ProcessBuilder(
            "java", "-cp",
            classpath,
            "com.waicool20.wai2k.LauncherKt",
            *args
        ).directory(appPath.toFile()).inheritIO().start()
        process.waitFor()
        exitProcess(process.exitValue())
    }
}