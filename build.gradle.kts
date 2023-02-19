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

plugins {
    kotlin("jvm") version "1.8.0"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "com.waicool20.wai2k"

    repositories {
        mavenCentral()
    }
}

task("buildArtifacts") {
    dependsOn(
        ":modules:launcher:build",
        ":modules:core:build",
        ":modules:core:packLibs",
        ":modules:assets:packAssets"
    )
}

task("createArtifactChecksums") {
    doLast {
        fileTree(projectDir).apply {
            include("modules/**/build/artifacts/**")
            exclude("**/*.md5")
        }.forEach {
            Utils.md5sum(it)
        }
    }
}

task("prepareDeploy") {
    dependsOn("buildArtifacts", "createArtifactChecksums")
    tasks["createArtifactChecksums"].mustRunAfter("buildArtifacts")
}
