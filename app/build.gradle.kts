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
        buildConfig = true
    }

    // Two builds from one code base (docs/distribution.md):
    // - github: the family build from GitHub releases. Updates itself (the design system's
    //   self-update); the only flavor with INTERNET, used for nothing but that check.
    // - play: for Google Play. No INTERNET at all; Play updates it.
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("String", "GITHUB_REPO", "\"gabrielbr/Cochilo\"")
        }
        create("play") {
            dimension = "distribution"
        }
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
        // NapperImportTest's real-file check runs only when NAPPER_CSV points at an export.
        unitTests.all { test -> System.getenv("NAPPER_CSV")?.let { test.environment("NAPPER_CSV", it) } }
    }

    lint {
        checkReleaseBuilds = false
    }
}

// The permission promise, checked on every build: play asks for none; github asks for exactly the
// two that self-update needs. A library that sneaks in INTERNET fails the build here.
val allowedPermissions = mapOf(
    "play" to emptySet(),
    "github" to setOf("android.permission.INTERNET", "android.permission.REQUEST_INSTALL_PACKAGES"),
)
androidComponents {
    onVariants { variant ->
        val flavor = variant.flavorName ?: return@onVariants
        val allowed = allowedPermissions.getValue(flavor)
        val cap = variant.name.replaceFirstChar { it.uppercase() }
        val manifest = variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST)
        val check = tasks.register("check${cap}Permissions") {
            group = "verification"
            description = "Fails if the merged ${variant.name} manifest asks for a permission outside the allowed set."
            inputs.file(manifest)
            doLast {
                val text = manifest.get().asFile.readText()
                val found = Regex("""<uses-permission(?:-sdk-23)?\s[^>]*android:name="([^"]+)"""")
                    .findAll(text).map { it.groupValues[1] }
                    // AndroidX adds this signature-level permission to protect its own receivers.
                    .filterNot { it.endsWith(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") }
                    .toSet()
                check(found == allowed) { "${variant.name} permissions are $found, expected exactly $allowed" }
            }
        }
        tasks.matching { it.name == "assemble$cap" || it.name == "test${cap}UnitTest" }.configureEach { dependsOn(check) }
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
