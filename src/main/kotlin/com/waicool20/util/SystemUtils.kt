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

package com.waicool20.util

import java.io.FilePermission
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.security.Permission

/**
 * System utility functions
 */
object SystemUtils {

    /**
     * Load a library from a given path.
     *
     * @param path Path to the library
     * @param loadDirectly If true, it will attempt to load the library directly using the absolute
     * path, otherwise it will attempt to modify the `java.library.path` variable and load it by name.
     */
    fun loadLibrary(path: Path, loadDirectly: Boolean = false) = loadLibrary(listOf(path), loadDirectly)

    /**
     * Loads libraries from a given path.
     *
     * @param paths List containing path to the libraries
     * @param loadDirectly If true, it will attempt to load the libraries directly using the absolute
     * path, otherwise it will attempt to modify the `java.library.path` variable and load it by name.
     */
    fun loadLibrary(paths: List<Path>, loadDirectly: Boolean = false) {
        if (loadDirectly) {
            paths.forEach { System.load(it.toAbsolutePath().normalize().toString()) }
        } else {
            val separator = if (OS.isWindows()) ";" else ":"
            val libs = paths.map { it.toAbsolutePath().parent.toString() }.toMutableSet()
            libs.addAll(System.getProperty("java.library.path").split(separator).toSet())

            System.setProperty("java.library.path", libs.joinToString(separator))
            with(ClassLoader::class.java.getDeclaredField("sys_paths")) {
                isAccessible = true
                set(null, null)
            }

            paths.forEach {
                try {
                    val libName = it.fileName.toString().takeWhile { it != '.' }.let {
                        if (OS.isUnix()) it.replaceFirst("lib", "") else it
                    }
                    System.loadLibrary(libName)
                } catch (e: UnsatisfiedLinkError) {
                    System.load(it.toAbsolutePath().normalize().toString())
                }
            }
        }
    }

    private val classLoader by lazy { ClassLoader.getSystemClassLoader() as URLClassLoader }
    private val loaderMethod by lazy { URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java) }

    /**
     * Load a Jar library into classpath.
     *
     * @param jar Path to Jar
     */
    fun loadJarLibrary(jar: Path) = loadJarLibrary(listOf(jar))

    /**
     * Loads Jar libraries into classpath.
     *
     * @param jars List containing path to the jars
     */
    fun loadJarLibrary(jars: List<Path>) {
        jars.map { it.toUri().toURL() }.forEach {
            if (!classLoader.urLs.contains(it)) {
                loaderMethod.isAccessible = true
                loaderMethod.invoke(classLoader, it)
            }
        }
    }

    /**
     * Gets the name of the class which contains the main entry point function.
     */
    val mainClassName: String by lazy {
        try {
            throw Exception()
        } catch (e: Exception) {
            e.stackTrace.last().className
        }
    }
}

/**
 * Operating system related utility functions and checks.
 */
object OS {
    /**
     * 32 bit architecture check.
     *
     * @return True if system architecture is 32 bit.
     */
    fun is32Bit() = !is64Bit()

    /**
     * 64 bit architecture check.
     *
     * @return True if system architecture is 64 bit.
     */
    fun is64Bit() = System.getProperty("os.arch").contains("64")

    /**
     * Windows system check.
     *
     * @return True if system is running a Windows OS.
     */
    fun isWindows() = System.getProperty("os.name").toLowerCase().contains("win")

    /**
     * Linux system check.
     *
     * @return True if system is running a Linux distro.
     */
    fun isLinux() = System.getProperty("os.name").toLowerCase().contains("linux")

    /**
     * Mac system check.
     *
     * @return True if system is running a version of MacOs.
     */
    fun isMac() = System.getProperty("os.name").toLowerCase().contains("mac")

    /**
     * Unix system check.
     *
     * @return True if system is running a UNIX like OS.
     */
    fun isUnix() = !isWindows()

    /**
     * DOS system check.
     *
     * @return True if system is running DOS like system.
     */
    fun isDos() = isWindows()

    /**
     * System library extension format in the form of `.<extension>`.
     *
     * @return Library extension.
     * @throws IllegalStateException If the OS of the current system cannot be determined.
     */
    val libraryExtension by lazy {
        when {
            isLinux() -> ".so"
            isWindows() -> ".dll"
            isMac() -> ".dylib"
            else -> error("Unknown OS")
        }
    }
}

/**
 * Resolves give path with this path. Allows for usage like `Paths.get("foo") / "bar"`,
 * Which is identical to `Paths.get("foo").resolve("bar")`.
 *
 * @param path Path to resolve.
 * @return Resolved path.
 */
operator fun Path.div(path: String): Path = resolve(path)

class IllegalExitException : SecurityException()

/**
 * Prevents calls to System.exit() inside the given action lambda block
 *
 * @param action Lambda to be executed that prevents exit calls
 * @return Lambda result
 * @throws IllegalExitException in case action block tries exit
 */
fun <T> preventSystemExit(action: () -> T): T {
    val manager = System.getSecurityManager()
    val exitManager = object : SecurityManager() {
        override fun checkPermission(permission: Permission) {
            if (permission !is FilePermission && permission.name.contains("exitVM")) {
                throw IllegalExitException()
            }
        }
    }
    System.setSecurityManager(exitManager)
    return try {
        action().also { System.setSecurityManager(manager) }
    } catch (e: Throwable) {
        if (e.cause is IllegalExitException) {
            System.setSecurityManager(manager)
            throw IllegalExitException()
        }
        throw e
    }
}

/**
 * Prevents logging to system out
 *
 * @param action Lambda to be executed that should be silent
 * @return Lambda result
 */
fun <T> disableSystemOut(action: () -> T): T {
    val out = System.out
    System.setOut(null)
    return action().also { System.setOut(out) }
}
