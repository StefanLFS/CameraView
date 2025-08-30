import com.android.build.api.dsl.LibraryExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("jacoco")
}

android {
    namespace = "com.otaliastudios.cameraview"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        // Libraries shouldn't set targetSdk anymore (AGP warns), so we omit it.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["filter"] =
            "com.otaliastudios.cameraview.tools.SdkExcludeFilter," +
                    "com.otaliastudios.cameraview.tools.SdkIncludeFilter"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("debug") { isTestCoverageEnabled = true }
        getByName("release") { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-inline:2.28.2")

    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("org.mockito:mockito-android:2.28.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    api("androidx.exifinterface:exifinterface:1.3.3")
    api("androidx.lifecycle:lifecycle-common:2.3.1")
    api("com.google.android.gms:play-services-tasks:17.2.1")
    implementation("androidx.annotation:annotation:1.2.0")

    // Use the published Egloo for now so we can build/run
    implementation("com.otaliastudios.opengl:egloo:0.6.1")
}

/* ---- Code Coverage tasks (unchanged, but updated to avoid deprecated buildDir getter use) ---- */

val coverageInputDir = layout.buildDirectory.dir("coverage_input")
val coverageOutputDir = layout.buildDirectory.dir("coverage_output")

tasks.register("runUnitTests") {
    dependsOn("testDebugUnitTest")
    doLast {
        copy {
            from(layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))
            into(coverageInputDir.map { it.dir("unit_tests") })
        }
    }
}

tasks.register("runAndroidTests") {
    dependsOn("connectedDebugAndroidTest")
    doLast {
        copy {
            from(layout.buildDirectory.dir("outputs/code_coverage/debugAndroidTest/connected"))
            include("*coverage.ec")
            into(coverageInputDir.map { it.dir("android_tests") })
        }
    }
}

jacoco { toolVersion = "0.8.5" }

tasks.register("computeCoverage", JacocoReport::class) {
    dependsOn("compileDebugSources")
    executionData.from(fileTree(coverageInputDir))
    sourceDirectories.from(android.sourceSets["main"].java.srcDirs)
    additionalSourceDirs.from(
        layout.buildDirectory.dir("generated/source/buildConfig/debug"),
        layout.buildDirectory.dir("generated/source/r/debug")
    )
    classDirectories.from(fileTree(layout.buildDirectory.dir("intermediates/javac/debug")) {
        exclude(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "android/**",
            "androidx/**",
            "com/google/**",
            "**/*\$ViewInjector*.*",
            "**/Dagger*Component.class",
            "**/Dagger*Component\$Builder.class",
            "**/*Module_*Factory.class",
            "**/com/otaliastudios/cameraview/filters/**.*"
        )
    })
    reports {
        html.required.set(true)
        xml.required.set(true)
        html.outputLocation.set(coverageOutputDir.map { it.dir("html") })
        xml.outputLocation.set(coverageOutputDir.map { it.file("xml/report.xml") })
    }
}
