plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun getVersionFromGit(): String {
    return try {
        val process = Runtime.getRuntime().exec("git describe --tags --abbrev=0")
        process.inputStream.bufferedReader().readText().trim()
    } catch (e: Exception) {
        "1.0"
    }
}

android {
    namespace = "com.paulohenriquesg.fahrenheit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.paulohenriquesg.fahrenheit"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = System.getenv("GITHUB_REF")?.split("/")?.last() ?: getVersionFromGit()

    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "path/to/local/keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "localKeystorePassword"
            keyAlias = System.getenv("KEY_ALIAS") ?: "localKeyAlias"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "localKeyPassword"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")

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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    // handle extra icons
    implementation(libs.androidx.material.icons.extended)

    // handle API
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // handle api logs
    implementation(libs.logging.interceptor)

    // handle cards
    implementation(libs.coil.compose)

    // handle media controls and player
    implementation(libs.androidx.media)
    implementation(libs.androidx.media2.session)

    // handle UI
    implementation(libs.androidx.material3)
    implementation(libs.androidx.tv.material.v100alpha01)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Encrypted SharedPreferences for secure token storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.leanback)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}