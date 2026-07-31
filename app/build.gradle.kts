import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.houvven.guise"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = namespace
        minSdk = 29
        targetSdk = 37
        val versionConfig = getVersionConfig()
        versionCode = versionConfig["versionCode"].toString().toInt()
        versionName = versionConfig["versionName"].toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += "arm64-v8a"
        }

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "/kotlin/**"
            excludes += "/*.txt"
            excludes += "/*.bin"
        }
    }
    lint {
        // MMKV 2 only ships 64-bit Android binaries; this app intentionally targets arm64 devices.
        disable += "ChromeOsAbiSupport"
    }
}

tasks.register("updateVersion") {
    doLast {
        val versionConfig = Properties()
        val file = rootProject.file("./app/version.properties")
        file.inputStream().use { versionConfig.load(it) }

        val versionCode = versionConfig["versionCode"].toString().toInt()
        versionConfig["versionCode"] = (versionCode + 1).toString()
        val versionName = versionConfig["versionName"].toString()
        val versionNameSplit = versionName.split(".")
        val (major, minor, patch) = versionNameSplit
        versionConfig["versionName"] = when {
            patch.toInt() < 9 -> "$major.$minor.${patch.toInt() + 1}"
            minor.toInt() < 9 -> "$major.${minor.toInt() + 1}.0"
            else -> "${major.toInt() + 1}.0.0"
        }
        file.outputStream().use { versionConfig.store(it, null) }
        println("Version updated to ${versionConfig["versionName"]}")
    }
}

dependencies {

    val roomVersion = "2.8.4"


    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")
    implementation(project(":ktx-xposed"))
    implementation("com.tencent:mmkv:2.4.1")
    // implementation("io.github.admin4j:http:0.4.0")

    // Kotlin-serilization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")


    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("com.materialkolor:material-kolor:4.1.1")

    implementation("androidx.navigation:navigation-compose:2.10.0-alpha06")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "false")
}

fun getVersionConfig(): Map<*, *> {
    val versionConfig = Properties()
    rootProject.file("app/version.properties").inputStream().use {
        versionConfig.load(it)
        return versionConfig.toMap()
    }
}
