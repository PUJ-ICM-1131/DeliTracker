import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val useAuthEmulator = providers.gradleProperty("FIREBASE_AUTH_EMULATOR").orNull == "true"
val emulatorHost = providers.gradleProperty("FIREBASE_EMULATOR_HOST").getOrElse("10.0.2.2")
require(emulatorHost.matches(Regex("[A-Za-z0-9.:-]+"))) { "FIREBASE_EMULATOR_HOST no es válido" }
// Clave de Geoapify (lugares y rutas): se lee de local.properties (que Git ignora) o de una propiedad de Gradle
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val geoapifyApiKey = (localProperties.getProperty("GEOAPIFY_API_KEY") ?: providers.gradleProperty("GEOAPIFY_API_KEY").orNull ?: "").trim()
require(geoapifyApiKey.matches(Regex("[A-Za-z0-9_.=+/-]*"))) { "GEOAPIFY_API_KEY tiene caracteres no válidos" }
if (file("google-services.json").isFile && !useAuthEmulator) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.delitracker.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.delitracker.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "FIREBASE_AUTH_EMULATOR", "false")
        buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"$emulatorHost\"")
        buildConfigField("String", "GEOAPIFY_API_KEY", "\"$geoapifyApiKey\"")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "FIREBASE_AUTH_EMULATOR", useAuthEmulator.toString())
            if (useAuthEmulator) applicationIdSuffix = ".local"
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val verifyFirebaseConfiguration by tasks.registering {
    doLast {
        check(!useAuthEmulator) { "Una entrega release no puede usar Firebase local" }
        check(file("google-services.json").isFile) {
            "Falta app/google-services.json del proyecto Firebase. No se puede generar una entrega conectada a la nube."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(verifyFirebaseConfiguration)
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
