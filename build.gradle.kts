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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    java
    kotlin("jvm") version "1.3.72"
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

group = "com.waicool20"
version = "v0.0.1"
defaultTasks = mutableListOf("build")

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    val versions = object {
        val Kotlin by lazy { plugins.getPlugin(KotlinPluginWrapper::class).kotlinPluginVersion }
        val KotlinCoroutines = "1.3.5"
        val Jackson = "2.10.1"
    }

    implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", versions.Kotlin)
    implementation("org.jetbrains.kotlin", "kotlin-reflect", versions.Kotlin)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", versions.KotlinCoroutines)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-javafx", versions.KotlinCoroutines)
    implementation("no.tornado", "tornadofx", "1.7.19")
    implementation("com.fasterxml.jackson.core", "jackson-core", versions.Jackson)
    implementation("com.fasterxml.jackson.core", "jackson-databind", versions.Jackson)
    implementation("com.fasterxml.jackson.core", "jackson-annotations", versions.Jackson)
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", versions.Jackson)
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", versions.Jackson)
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("org.controlsfx", "controlsfx", "8.40.14")
    implementation("org.reflections", "reflections", "0.9.11")

    implementation("net.sourceforge.tess4j", "tess4j", "4.4.0") {
        exclude("org.ghost4j")
        exclude("org.apache.pdfbox")
        exclude("org.jboss")
    }

    implementation("com.waicool20:waicoolUtils")
    implementation("com.waicool20.cvauto:android")
}

tasks {
    processResources { dependsOn("versioning") }
    build { finalizedBy("shadowJar") }
    jar {
        enabled = false
        manifest {
            attributes(mapOf("Main-Class" to "com.waicool20.wai2k.Wai2K"))
        }
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
            freeCompilerArgs = listOf(
                    "-Xuse-experimental=kotlin.Experimental",
                    "-Xuse-experimental=kotlin.time.ExperimentalTime",
                    "-Xuse-experimental=kotlinx.coroutines.FlowPreview",
                    "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-Xuse-experimental=kotlinx.coroutines.ObsoleteCoroutinesApi"
            )
        }
    }
    withType<ShadowJar> {
        archiveClassifier.value("")
        archiveVersion.value("")
        exclude("tessdata/")
    }
}

task("versioning") {
    val file = Paths.get("$projectDir/src/main/resources/version.txt")
    val content = """
    |{
    |    "version": "${project.version}"
    |}
    """.trimMargin()
    Files.write(file, content.toByteArray())
}

task<Zip>("packAssets") {
    archiveFileName.set("assets.zip")
    destinationDirectory.set(file("$buildDir"))

    from("$projectDir/assets")
}