import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun loadProperties(name: String) = Properties().apply {
    val file = rootProject.file(name)
    if (file.exists()) file.inputStream().use { load(it) }
}

// IDs reales de AdMob en admob.properties (no se sube a git). Sin ellos se usan los de prueba de Google.
val admob = loadProperties("admob.properties")
val testAdIds = mapOf(
    "APP_ID" to "ca-app-pub-3940256099942544~3347511713",
    "BANNER" to "ca-app-pub-3940256099942544/9214589741",
    "INTERSTITIAL" to "ca-app-pub-3940256099942544/1033173712",
    "APP_OPEN" to "ca-app-pub-3940256099942544/9257395921",
    "REWARDED" to "ca-app-pub-3940256099942544/5224354917",
)
val adUnits = listOf("BANNER", "INTERSTITIAL", "APP_OPEN", "REWARDED")
val keystoreProperties = loadProperties("keystore.properties")

android {
    namespace = "com.testsoposiciones.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.testsoposiciones.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdIds.getValue("APP_ID")
            adUnits.forEach { buildConfigField("String", "AD_$it", "\"${testAdIds.getValue(it)}\"") }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
            manifestPlaceholders["admobAppId"] = admob.getProperty("APP_ID", testAdIds.getValue("APP_ID"))
            adUnits.forEach {
                buildConfigField("String", "AD_$it", "\"${admob.getProperty(it, testAdIds.getValue(it))}\"")
            }
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.billing)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
