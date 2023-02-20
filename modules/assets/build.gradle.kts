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

tasks {
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        destinationDirectory.set(file("$buildDir/artifacts/"))
        from(projectDir)
        into("assets")

        exclude("build/", "build.gradle.kts")
    }
}

task<Zip>("packAssets") {
    archiveFileName.set("assets.zip")

    exclude("/models/**")
}

task<Zip>("packModels") {
    archiveFileName.set("models.zip")

    include("/models/**")
}
