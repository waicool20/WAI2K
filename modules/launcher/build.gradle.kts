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
import org.gradle.configurationcache.extensions.capitalized

plugins {
    id("com.github.johnrengelman.shadow") version "latest.release"
}

version = "0.0.1"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
}

kotlin {
    jvmToolchain(11)
}

tasks {
    build {
        dependsOn("shadowJar")
    }
    jar {
        enabled = false
    }
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    withType<ShadowJar> {
        minimize()
        archiveFileName.set("${rootProject.name}-${project.name.capitalized()}.jar")
        archiveClassifier.set("")
        archiveVersion.set("")
        destinationDirectory.set(file("$${layout.buildDirectory}/artifacts/"))
        manifest { attributes(mapOf("Main-Class" to "com.waicool20.wai2k.launcher.Main")) }
    }
}
