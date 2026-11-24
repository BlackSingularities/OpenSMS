import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appVersionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
val appVersionName = (project.findProperty("versionName") as String?) ?: "0.1.0-dev"
val keystorePath: String? = System.getenv("SIGNING_KEYSTORE_PATH")

/**
 * Języki interfejsu wykrywane z katalogów zasobów: każdy `res/values-xx/strings.xml`
 * to jeden język (angielski w `values/` jest domyślny). Nowy język = nowy plik, nic więcej.
 */
val appLocales: List<String> = listOf("en") + (file("src/main/res").listFiles().orEmpty()
    .filter { it.isDirectory && it.name.startsWith("values-") && File(it, "strings.xml").exists() }
    .mapNotNull { dir ->
        val q = dir.name.removePrefix("values-")
        when {
            q.startsWith("b+") -> q.removePrefix("b+").replace('+', '-')
            Regex("[a-z]{2,3}(-r[A-Z]{2})?").matches(q) -> q.replace("-r", "-")
            else -> null
        }
    }
    .sorted())

android {
    namespace = "com.blacksingularities.opensms"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.blacksingularities.opensms"
        minSdk = 29
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "UPDATE_REPO", "\"BlackSingularities/OpenSMS\"")
        buildConfigField("String[]", "LOCALES", appLocales.joinToString(", ", "{", "}") { "\"$it\"" })
    }

    signingConfigs {
        create("release") {
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storeType = "pkcs12"
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystorePath != null && file(keystorePath).exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources {
        // Lista języków dla systemu (wybór języka aplikacji w Androidzie 13+) generowana z katalogów values-xx.
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.01.00")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
