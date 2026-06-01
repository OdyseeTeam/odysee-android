import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.odysee.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.odysee.app"
        minSdk = 30
        targetSdk = 36
        // versionCode continues the Cordova app's numbering so that Cordova
        // installs treat the native build as an update and migrate.
        // Cordova's apk.odysee.tv/release.json was at 124 when the native
        // app shipped; 125 is the first native release. Bump monotonically.
        versionCode = 126
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            applicationId = "com.odysee.app"
            buildConfigField("boolean", "BUILT_IN_UPDATER", "false")
        }
        create("apk") {
            dimension = "distribution"
            applicationId = "com.odysee.app"
            buildConfigField("boolean", "BUILT_IN_UPDATER", "true")
        }
        create("foss") {
            dimension = "distribution"
            applicationId = "com.odysee.floss"
            versionNameSuffix = "-foss"
            buildConfigField("boolean", "BUILT_IN_UPDATER", "false")
        }
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }

    androidComponents {
        beforeVariants(selector().withFlavor("distribution" to "foss")) { variant ->
            variant.androidTest?.enable = false
        }
        onVariants(selector().withFlavor("distribution" to "foss")) { variant ->
            project.afterEvaluate {
                tasks.matching {
                    val n = it.name
                    n.contains(variant.name, ignoreCase = true) &&
                        (n.contains("GoogleServices", ignoreCase = true) ||
                            n.contains("Crashlytics", ignoreCase = true))
                }.configureEach { enabled = false }
            }
        }
    }

    sourceSets {
        getByName("play") {
            java.srcDir("src/playAndApk/java")
            kotlin.srcDir("src/playAndApk/java")
            res.srcDir("src/playAndApk/res")
        }
        getByName("apk") {
            java.srcDir("src/playAndApk/java")
            kotlin.srcDir("src/playAndApk/java")
            res.srcDir("src/playAndApk/res")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":core:database"))
    implementation(project(":feature:home"))
    implementation(project(":feature:channel"))
    implementation(project(":feature:search"))
    implementation(project(":feature:wallet"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:library"))
    implementation(project(":feature:shorts"))

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.haze)
    implementation(libs.haze.materials)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Play / APK flavor Firebase deps. Requires google-services.json in the
    // respective src/<flavor>/ directory and applying the google-services +
    // crashlytics plugins (added later).
    "playImplementation"(platform(libs.firebase.bom))
    "playImplementation"(libs.firebase.analytics)
    "playImplementation"(libs.firebase.crashlytics)
    "playImplementation"(libs.firebase.messaging)
    "apkImplementation"(platform(libs.firebase.bom))
    "apkImplementation"(libs.firebase.analytics)
    "apkImplementation"(libs.firebase.crashlytics)
    "apkImplementation"(libs.firebase.messaging)

    // Chromecast (Google Play services) — play and apk variants. FOSS gets a stub.
    "playImplementation"(libs.play.services.cast.framework)
    "playImplementation"(libs.androidx.mediarouter)
    "apkImplementation"(libs.play.services.cast.framework)
    "apkImplementation"(libs.androidx.mediarouter)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
