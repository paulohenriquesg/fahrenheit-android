plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.paulohenriquesg.fahrenheit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.paulohenriquesg.fahrenheit"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = System.getenv("GITHUB_REF")?.split("/")?.last() ?: "1.0"

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
        compose = true
    }
}

dependencies {
    // handle extra icons
    implementation("androidx.compose.material:material-icons-extended:1.0.5")

    // handle API
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // handle api logs
    implementation(libs.logging.interceptor)

    // handle cards
    implementation("io.coil-kt:coil-compose:2.2.2")

    // handle media controls and player
    implementation("androidx.media:media:1.6.0")
    implementation("androidx.media2:media2-session:1.2.1")

    // handle UI
    implementation(libs.androidx.material3)
    implementation(libs.androidx.tv.material.v100alpha01)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}