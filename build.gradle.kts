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
