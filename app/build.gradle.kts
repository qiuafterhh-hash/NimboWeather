import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
val owmKey: String = localProps.getProperty("OPENWEATHER_API_KEY") ?: ""

// --- Version management: derive versionName/versionCode from the latest git tag ---
// Tag `vX.Y.Z` -> versionName "X.Y.Z", versionCode X*1_000_000 + Y*1_000 + Z (monotonic).
// Falls back to FALLBACK_VERSION_NAME when no tag is reachable (e.g. shallow CI clones
// for debug builds); the release workflow checks out with full history so tags resolve.
val FALLBACK_VERSION_NAME = "0.1.0"

fun latestGitTag(): String? = try {
    val proc = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val out = proc.inputStream.bufferedReader().readText().trim()
    if (proc.waitFor() == 0 && out.isNotEmpty()) out else null
} catch (_: Exception) {
    null
}

val appVersionName: String = (latestGitTag()?.removePrefix("v") ?: FALLBACK_VERSION_NAME)

val appVersionCode: Int = Regex("""(\d+)\.(\d+)\.(\d+)""").find(appVersionName)?.let { m ->
    val (maj, min, pat) = m.destructured
    maj.toInt() * 1_000_000 + min.toInt() * 1_000 + pat.toInt()
} ?: 1

android {
    namespace = "com.nimboweather.forecast"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nimboweather.forecast"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$owmKey\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Networking — OpenWeatherMap
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Ads — AdMob (mediation entry; wrapped by our AdMediator abstraction)
    implementation("com.google.android.gms:play-services-ads:23.5.0")
    // UMP — GDPR/CCPA consent (no Firebase required)
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    // M1 — location, lists, coroutines<->Task bridge
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("io.coil-kt:coil:2.7.0")

    // L2 weather radar — OpenStreetMap base + RainViewer tile overlay (no Google Maps key)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    testImplementation("junit:junit:4.13.2")

    // Instrumented (on-device) UI tests — Espresso + AndroidX Test
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
}
