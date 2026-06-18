plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace  = "com.gseterminal.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gseterminal.app"
        minSdk        = 24          // Android 7.0 — covers ~97 % of active devices
        targetSdk     = 34
        versionCode   = 1
        versionName   = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // ── RELEASE SIGNING ─────────────────────────────────────────────────
        // Add these keys to your local.properties (never commit to git):
        //   KEYSTORE_FILE=../keystore/gse_release.jks
        //   KEY_ALIAS=gse_key
        //   KEY_PASSWORD=yourKeyPassword
        //   STORE_PASSWORD=yourStorePassword
        // ─────────────────────────────────────────────────────────────────────
        create("release") {
            val props = rootProject.file("local.properties")
                .takeIf { it.exists() }
                ?.let { java.util.Properties().also { p -> p.load(it.inputStream()) } }

            storeFile     = props?.getProperty("KEYSTORE_FILE")?.let { file(it) }
            storePassword = props?.getProperty("STORE_PASSWORD") ?: ""
            keyAlias      = props?.getProperty("KEY_ALIAS")      ?: ""
            keyPassword   = props?.getProperty("KEY_PASSWORD")   ?: ""
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
            isDebuggable        = true
        }
        release {
            isMinifyEnabled    = true
            isShrinkResources  = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
        viewBinding = true
        buildConfig = true
    }

    // Bundle HTML + assets
    sourceSets["main"].assets.srcDirs("src/main/assets")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)   // pull-to-refresh
    implementation(libs.androidx.webkit)               // WebViewCompat, SafeBrowsing, PostMessage
}
