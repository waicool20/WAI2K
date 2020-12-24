/*
 * The MIT License (MIT)
 *
 * Copyright (c) waicoolUtils by waicool20
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

plugins {
    java
    kotlin("jvm") version "1.4.20"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "com.waicool20"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    val versions = object {
        val Kotlin by lazy { plugins.getPlugin(KotlinPluginWrapper::class).kotlinPluginVersion }
    }

    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-maven:3.1.4")

    /* --- */
    testImplementation("junit", "junit", "4.12")
}

tasks {
    build { finalizedBy("shadowJar") }
    jar {
        enabled = false
        manifest {
            attributes(mapOf("Main-Class" to "com.waicool20.wai2k.launcher.Main"))
        }
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    withType<ShadowJar> {
        minimize {
            exclude(dependency("org.jboss.shrinkwrap.resolver:.*:.*"))
        }
        archiveClassifier.value("")
        archiveVersion.value("")
        exclude("kotlin/reflect/**")
        doLast { md5sum(archiveFile.get()) }
    }
}

fun md5sum(file: RegularFile) {
    val path = file.asFile.toPath()
    val md5File = Paths.get("$path.md5")
    val md5sum = MessageDigest.getInstance("MD5")
        .digest(Files.readAllBytes(path))
        .joinToString("") { String.format("%02x", it) }
    Files.write(md5File, md5sum.toByteArray())
}
