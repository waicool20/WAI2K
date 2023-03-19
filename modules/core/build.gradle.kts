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

import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("com.github.johnrengelman.shadow") version "latest.release"
    id("org.openjfx.javafxplugin") version "latest.release"
}

version =
    System.getenv("GITHUB_SHA")?.take(8) ?: System.getenv("APPVEYOR_REPO_COMMIT")?.take(8) ?: "dev"

javafx {
    version = "19"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    val versions = object {
        val KotlinCoroutines = "1.6.4"
        val Jackson = "2.14.2"
        val OpenJfx = "16"
        val Ktor = "2.2.3"
    }

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Kotlin scripting
    implementation(kotlin("scripting-jvm", kotlin.coreLibrariesVersion))
    implementation(kotlin("scripting-jvm-host", kotlin.coreLibrariesVersion))
    implementation(kotlin("scripting-common", kotlin.coreLibrariesVersion))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.KotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:${versions.KotlinCoroutines}")
    implementation("com.fasterxml.jackson.core:jackson-core:${versions.Jackson}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${versions.Jackson}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:${versions.Jackson}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${versions.Jackson}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${versions.Jackson}")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("no.tornado:tornadofx:2.0.0-SNAPSHOT")
    implementation("org.controlsfx:controlsfx:11.1.2")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("ai.djl.pytorch:pytorch-engine:0.21.0")
    implementation("com.github.ajalt.clikt:clikt-jvm:3.5.2")

    implementation("net.sourceforge.tess4j", "tess4j", "5.2.0") {
        exclude("org.ghost4j")
        exclude("org.apache.pdfbox")
        exclude("org.jboss")
    }

    val platforms = if (System.getenv("CI").equals("true", ignoreCase = true)) {
        listOf("win", "linux", "mac")
    } else listOf(null)

    platforms.forEach { p ->
        implementation("org.openjfx", "javafx-base", versions.OpenJfx, classifier = p)
        implementation("org.openjfx", "javafx-controls", versions.OpenJfx, classifier = p)
        implementation("org.openjfx", "javafx-fxml", versions.OpenJfx, classifier = p)
        implementation("org.openjfx", "javafx-graphics", versions.OpenJfx, classifier = p)
        implementation("org.openjfx", "javafx-media", versions.OpenJfx, classifier = p)
        implementation("org.openjfx", "javafx-swing", versions.OpenJfx, classifier = p)
        implementation("org.openjfx", "javafx-web", versions.OpenJfx, classifier = p)
    }

    api("com.waicool20:waicoolUtils")
    api("com.waicool20.cvauto:android")

    implementation("io.ktor", "ktor-server-core", versions.Ktor)
    implementation("io.ktor", "ktor-server-netty", versions.Ktor)
    implementation("io.ktor", "ktor-server-cors", versions.Ktor)
    implementation("io.ktor", "ktor-server-content-negotiation", versions.Ktor)
    implementation("io.ktor", "ktor-server-websockets", versions.Ktor)
    implementation("io.ktor", "ktor-serialization-jackson", versions.Ktor)
}

tasks {
    processResources {
        dependsOn("versioning")
    }
    build {
        dependsOn("shadowJar")
    }
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    jar {
        enabled = false
    }
    shadowJar {
        archiveFileName.set("${rootProject.name}-${project.name.capitalized()}.jar")
        archiveClassifier.set("")
        archiveVersion.set("")
        destinationDirectory.set(file("$buildDir/artifacts/"))
        manifest { attributes(mapOf("Main-Class" to "com.waicool20.wai2k.Wai2kKt")) }
        dependencies { include { it.moduleGroup.startsWith("com.waicool20") } }
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    withType<KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
    }
}

task<Zip>("packLibs") {
    archiveFileName.set("libs.zip")
    destinationDirectory.set(file("$buildDir/artifacts/"))
    from(project.configurations.filter { it.isCanBeResolved })
    exclude { f -> gradle.includedBuilds.any { b -> f.name.contains(b.name, true) } }
    into("libs")
}

task("versioning") {
    doLast {
        val file = Paths.get("$projectDir/src/main/resources/version.txt")
        Files.write(file, project.version.toString().toByteArray())
    }
}
