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
import java.nio.file.StandardOpenOption

plugins {
    java
    kotlin("jvm") version "1.4.0"
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
        val KotlinCoroutines = "1.3.9"
        val Jackson = "2.10.1" // Higher version break loading javafx compatibility
    }

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
    implementation("org.reflections", "reflections", "0.9.12")
    implementation("com.squareup.okhttp3:okhttp:4.7.2")

    implementation("net.sourceforge.tess4j", "tess4j", "4.5.1") {
        exclude("org.ghost4j")
        exclude("org.apache.pdfbox")
        exclude("org.jboss")
    }

    implementation("com.waicool20:waicoolUtils")
    implementation("com.waicool20:cvauto-android")
}

tasks {
    processResources {
        dependsOn("versioning")
        dependsOn("deps-list")
    }
    build {
        finalizedBy("shadowJar")
        finalizedBy(gradle.includedBuild("launcher").task(":build"))
    }
    jar {
        enabled = false
        manifest {
            attributes(mapOf("Main-Class" to "com.waicool20.wai2k.Wai2K"))
        }
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
            freeCompilerArgs = listOf()
        }
    }
    withType<ShadowJar> {
        archiveClassifier.value("")
        archiveVersion.value("")
        dependencies {
            include { it.moduleGroup.startsWith("com.waicool20") }
        }
    }
}

task("deps-list") {
    val file = Paths.get("$projectDir/build/dependencies.txt")
    doFirst {
        if (Files.notExists(file)) {
            Files.createDirectories(file.parent)
            Files.createFile(file)
        }
    }
    doLast {
        tailrec fun getDeps(deps: Set<DependencyResult>): List<DependencyResult> {
            return if (deps.isNotEmpty()) deps.flatMap { it.from.dependencies } else getDeps(deps)
        }

        val deps = getDeps(configurations.default.get().incoming.resolutionResult.allDependencies)
                .map { it.toString() }
                .distinct()
                .filterNot { it.startsWith("project") || it.contains("->") }
        val repos = project.repositories.mapNotNull { it as? MavenArtifactRepository }.map { it.url }
        val output = StringBuilder()
        output.appendln("Repositories:")
        repos.forEach { output.appendln("- $it") }
        output.appendln()
        output.appendln("Dependencies:")
        deps.forEach { output.appendln("- $it") }
        Files.write(file, output.toString().toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
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