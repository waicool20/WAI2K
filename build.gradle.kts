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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

plugins {
    kotlin("jvm") version "1.6.0"
    id("com.github.johnrengelman.shadow") version "latest.release"
    id("org.openjfx.javafxplugin") version "latest.release"
}

group = "com.waicool20"
version = System.getenv("APPVEYOR_REPO_COMMIT")?.take(8) ?: "dev"
defaultTasks = mutableListOf("build")

javafx {
    version = "16"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    val versions = object {
        val KotlinCoroutines = "1.5.1"
        val Jackson = "2.12.3"
        val OpenJfx = "16"
    }

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", versions.KotlinCoroutines)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-javafx", versions.KotlinCoroutines)
    implementation("com.fasterxml.jackson.core", "jackson-core", versions.Jackson)
    implementation("com.fasterxml.jackson.core", "jackson-databind", versions.Jackson)
    implementation("com.fasterxml.jackson.core", "jackson-annotations", versions.Jackson)
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", versions.Jackson)
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", versions.Jackson)
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("no.tornado:tornadofx:2.0.0-SNAPSHOT")
    implementation("org.controlsfx:controlsfx:11.1.1")
    implementation("org.reflections", "reflections", "0.9.12")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("ai.djl.pytorch:pytorch-engine:0.15.0")
    implementation("ai.djl.pytorch:pytorch-native-auto:1.9.1")

    implementation("net.sourceforge.tess4j", "tess4j", "4.5.4") {
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

    implementation("com.waicool20:waicoolUtils")
    implementation("com.waicool20:cvauto-android")
}

tasks {
    processResources {
        dependsOn("versioning")
    }
    build {
        dependsOn(gradle.includedBuild("launcher").task(":build"))
        finalizedBy("shadowJar")
    }
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    jar {
        enabled = false
    }
    shadowJar {
        archiveClassifier.value("")
        archiveVersion.value("")
        manifest { attributes(mapOf("Main-Class" to "com.waicool20.wai2k.LauncherKt")) }
        dependencies { include { it.moduleGroup.startsWith("com.waicool20") } }
        doLast { md5sum(archiveFile.get()) }
    }
    withType<KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
}

task("prepareDeploy") {
    dependsOn("build", "packLibs", "packAssets")
}

task<Copy>("copyLibs") {
    from(project.configurations.filter { it.isCanBeResolved })
    exclude { f -> gradle.includedBuilds.any { b -> f.name.contains(b.name, true) } }
    into("$buildDir/tmp/libs/")
}

task<Zip>("packLibs") {
    dependsOn("copyLibs")
    archiveFileName.set("libs.zip")
    destinationDirectory.set(file("$buildDir/deploy/"))
    from("$buildDir/tmp/")
    include("/libs/**")
    doLast { md5sum(archiveFile.get()) }
}

task("versioning") {
    val file = Paths.get("$projectDir/src/main/resources/version.txt")
    Files.write(file, project.version.toString().toByteArray())
}

task<Zip>("packAssets") {
    archiveFileName.set("assets.zip")
    destinationDirectory.set(file("$buildDir/deploy/"))

    from(projectDir)
    include("/assets/**")
    exclude("/assets/models/**")
    doLast { md5sum(archiveFile.get()) }
}

task<Zip>("packModels") {
    archiveFileName.set("models.zip")
    destinationDirectory.set(file("$buildDir/deploy/"))

    from(projectDir)
    include("/assets/models/**")
    doLast { md5sum(archiveFile.get()) }
}

fun md5sum(file: RegularFile) {
    val path = file.asFile.toPath()
    val md5File = Paths.get("$path.md5")
    val md5sum = MessageDigest.getInstance("MD5")
        .digest(Files.readAllBytes(path))
        .joinToString("") { String.format("%02x", it) }
    Files.write(md5File, md5sum.toByteArray())
}