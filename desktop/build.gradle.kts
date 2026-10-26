import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
}

// single source of truth for the version (shared with the Android app)
val versionFile = rootProject.file("version.properties")
val projectVersion: String = Properties().apply {
    versionFile.inputStream().use { load(it) }
}.getProperty("versionName")

val generatedVersionDir = layout.buildDirectory.dir("generated/version")
val generateVersion by tasks.registering {
    val version = projectVersion
    val outputDir = generatedVersionDir
    inputs.property("version", version)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get()
            .file("com/elishaazaria/sayboard/desktop/Version.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """package com.elishaazaria.sayboard.desktop

/** GENERATED from version.properties - do not edit. */
object Version {
    const val NAME = "$version"
}
"""
        )
    }
}
kotlin.sourceSets["main"].kotlin.srcDir(generateVersion)

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // speech recognition
    implementation("com.alphacephei:vosk:0.3.45")

    // native calls: SendInput on Windows, loading libvosk
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")

    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

compose.desktop {
    application {
        mainClass = "com.elishaazaria.sayboard.desktop.MainKt"

        nativeDistributions {
            // `deb`/`rpm` are created on Linux (rpm needs rpmbuild), `msi` on
            // Windows (needs WiX). AppImage: use ./package-appimage.sh.
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi)
            packageName = "sayTypist"
            packageVersion = projectVersion
            description = "sayTypist - offline voice typing with Vosk models"
            vendor = "sayTypist"
            modules("java.desktop", "java.logging", "jdk.unsupported", "java.sql")

            linux {
                shortcut = true
                menuGroup = "Utility"
                appCategory = "Utility"
                iconFile.set(project.file("icons/saytypist.png"))
            }
            windows {
                menu = true
                shortcut = true
                iconFile.set(project.file("icons/saytypist.ico"))
                dirChooser = true
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

/**
 * Regenerates app/src/main/java/.../ModelLink.kt, ModelSizeTier.kt and the
 * desktop copy of the catalog from catalog/vosk_models.json.
 */
tasks.register<JavaExec>("generateCatalog") {
    group = "build"
    description = "Regenerates the model catalog from catalog/vosk_models.json"
    mainClass.set("com.elishaazaria.sayboard.desktop.tools.GenerateCatalogKt")
    classpath = sourceSets["main"].runtimeClasspath
    args(rootProject.projectDir.absolutePath)
}
