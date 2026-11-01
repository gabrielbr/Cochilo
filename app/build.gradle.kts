plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("app.cash.paparazzi")
}

android {
    namespace = "br.gabriel.cochilo"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.gabriel.cochilo"
        minSdk = 26
        targetSdk = 35
        // CI sets these from the git tag (v0.1.0 -> 0.1.0); local builds use the literals.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "0.1.0"
    }

    signingConfigs {
        create("release") {
            // Personal test builds: a key checked into the repo, so every release installs over the
            // last one. Before sharing the app beyond the family, set the RELEASE_* secrets instead.
            val env = { k: String -> System.getenv(k)?.takeIf { it.isNotEmpty() } }
            storeFile = file(env("RELEASE_STORE_FILE") ?: "cochilo-test.keystore")
            storePassword = env("RELEASE_STORE_PASSWORD") ?: "android"
            keyAlias = env("RELEASE_KEY_ALIAS") ?: "cochilo"
            keyPassword = env("RELEASE_KEY_PASSWORD") ?: "android"
        }
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            // Not minified until it has been tried on a phone (R8 vs Room and serialization).
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    androidResources {
        // Per-app language (Android 13+) reads res/xml/locales_config.xml.
        generateLocaleConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        checkReleaseBuilds = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.core:core-ktx:1.15.0")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
}
