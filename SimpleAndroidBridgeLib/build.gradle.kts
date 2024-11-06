import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.gradle.jsgradlecompiler)
    alias(libs.plugins.maven.publish)
}

object JsVars {
    const val JS_IN = "./src/main/js/"
    const val JS_OUT = "./build/js/"
}

android {
    namespace = "de.andycandy.android.bridge"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    sourceSets {
        named("main") {
            assets.srcDirs(JsVars.JS_OUT)
        }
    }
}

dependencies {

    compileOnly(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.gson)
    implementation(kotlin("reflect"))
}

jsOptions {
    inputPath = file(JsVars.JS_IN).path
    outputPath = file(JsVars.JS_OUT).path
    compilationLevel = "SIMPLE_OPTIMIZATIONS"
    jsVersionIn = "ECMASCRIPT_2020"
    jsVersionOut = "ECMASCRIPT5"
    isCombineAllFiles = false
    isKeepSameName = false
}

tasks {
    preBuild {
        dependsOn("compileJs")
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.DEFAULT)

    coordinates("com.github.andycandy-de", "simple-android-bridge", "1.1.0")

    pom {
        name.set("simple-android-bridge")
        description.set("Build a bridge! This library is created to create a powerful interface between Android and Webapp.")
        url.set("https://github.com/andycandy-de/SimpleAndroidBridge")
        licenses {
            license {
                name.set("The MIT License (MIT)")
                url.set("https://raw.githubusercontent.com/andycandy-de/SimpleAndroidBridge/refs/heads/development/LICENSE")
            }
        }
        developers {
            developer {
                id.set("andycandy-de")
                name.set("AndyCandy")
                url.set("https://github.com/andycandy-de")
            }
        }
        scm {
            url.set("https://github.com/andycandy-de/SimpleAndroidBridge")
            connection.set("scm:git:https://github.com/andycandy-de/SimpleAndroidBridge")
            developerConnection.set("scm:git:https://github.com/andycandy-de/SimpleAndroidBridge")
        }
    }

    signAllPublications()
}