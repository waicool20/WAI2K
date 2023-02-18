val artifactsDir by extra(file("$buildDir/artifacts/"))

plugins {
    kotlin("jvm") version "1.8.0"
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "com.waicool20.wai2k"

    repositories {
        mavenCentral()
    }
}

task("prepareDeploy") {
    dependsOn(
        ":modules:launcher:build",
        ":modules:core:build",
        ":modules:core:packLibs",
        ":modules:assets:packAssets"
    )
}
