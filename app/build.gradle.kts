import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * The tag as one increasing number: v1.2.3 -> 10203. The in-app updater compares
 * these, and Android refuses to install an APK whose code is lower than the one
 * installed, so it must never go backwards. 1 when the tag cannot be read, which
 * keeps an untagged build below every release.
 */
fun versionCodeFromVersionName(versionName: String): Int {
    val parts = versionName.removePrefix("v").removePrefix("V").split(".")
    if (parts.size < 3) return 1
    val numbers = parts.take(3).map { it.takeWhile(Char::isDigit).toIntOrNull() ?: return 1 }
    require(numbers.all { it in 0..99 }) {
        "version $versionName does not fit major.minor.patch with each part under 100"
    }
    return numbers[0] * 10000 + numbers[1] * 100 + numbers[2]
}

// The newest version tag, read in the project directory. Without workingDir git
// ran wherever the Gradle daemon happened to start, and outside the repository
// that printed nothing and built versionCode 1 (#81). --match skips the moving
// "latest" tag the release workflow publishes, which is not a version.
val gitDescribe = providers.exec {
    workingDir = rootDir
    commandLine("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
    isIgnoreExitValue = true
}

// Why no tag could be read, or null when one was.
val missingTagReason: String? = try {
    val exitValue = gitDescribe.result.get().exitValue
    when {
        exitValue != 0 ->
            "git describe exited $exitValue: ${gitDescribe.standardError.asText.get().trim()}"
        gitDescribe.standardOutput.asText.get().isBlank() -> "git describe printed no tag"
        else -> null
    }
} catch (e: Exception) {
    "git could not be run: ${e.message}"
}

val appVersionName =
    if (missingTagReason == null) gitDescribe.standardOutput.asText.get().trim() else "untagged"
val appVersionCode = versionCodeFromVersionName(appVersionName)

// A release built without a tag would ship as versionCode 1, below every install,
// and the updater could never replace it, so anything that builds a release
// variant or feeds the update manifest refuses. Debug builds carry on, but say so.
gradle.taskGraph.whenReady {
    val reason = missingTagReason ?: return@whenReady
    val needsTag = allTasks.filter { it.name == "printVersionInfo" || it.name.contains("Release") }
    if (needsTag.isNotEmpty()) {
        throw GradleException(
            "No version tag could be read in $rootDir ($reason), so the versionCode would be " +
                "$appVersionCode. ${needsTag.first().path} needs a v-prefixed tag such as " +
                "v1.2.3 reachable from HEAD; fetch tags (git fetch --tags, or fetch-depth: 0 " +
                "in CI) or tag the commit."
        )
    }
    logger.warn(
        "w: No version tag could be read in $rootDir ($reason). Falling back to versionName " +
            "\"$appVersionName\", versionCode $appVersionCode, for this non-release build."
    )
}

// The release workflow reads these to build the update manifest, so the numbers
// in it cannot drift from the ones compiled into the APK.
tasks.register("printVersionInfo") {
    doLast { println("$appVersionCode $appVersionName") }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.paulohenriquesg.fahrenheit"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.paulohenriquesg.fahrenheit"
        minSdk = 25
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        debug {
            enableUnitTestCoverage = true
            // Its own id, so a debug build installs beside a released one
            // rather than demanding the release signing key to replace it.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
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
    testOptions {
        // android.util.Log and friends are stubs that throw on the JVM. Returning
        // defaults lets framework-free classes be tested with plain JUnit; anything
        // that actually depends on Android behaviour should use Robolectric.
        unitTests.isReturnDefaultValues = true
        // Hands Robolectric the merged manifest, so Compose UI tests can launch
        // the ComponentActivity that ui-test-manifest declares.
        unitTests.isIncludeAndroidResources = true

        unitTests.all {
            // Robolectric loads classes in its own sandbox classloader; without
            // this every Robolectric test contributes zero coverage, which reads
            // as "untested" when it is really "unmeasured".
            it.extensions.configure(JacocoTaskExtension::class.java) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
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

    // handle markdown rendering
    implementation(libs.commonmark)

    // handle media controls and player
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media2.session)

    // handle UI
    implementation(libs.androidx.material3)
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
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.androidx.media3.test.utils.robolectric)
    // Compose UI tests on the JVM via Robolectric, for UI that can be checked
    // without an emulator.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)

    // AndroidX Test dependencies
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}