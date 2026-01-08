plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.aplikacja_dla_strzelcow"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.aplikacja_dla_strzelcow"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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

    // --- Compose ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // --- Firebase (JEDEN BoM!) ---
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // --- Google Sign-In ---
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // --- CameraX ---
    implementation("androidx.camera:camera-camera2:1.5.2")
    implementation("androidx.camera:camera-lifecycle:1.5.2")
    implementation("androidx.camera:camera-view:1.5.2")
    implementation("androidx.camera:camera-core:1.5.2")
    implementation("com.google.guava:guava:33.5.0-android")

    // --- Coil (obrazy z URL – Firebase Storage) ---
    implementation("io.coil-kt:coil-compose:2.7.0")

    //OpenCv
    implementation(project(":opencv"))
    implementation(libs.androidx.exifinterface)

    implementation("com.google.code.gson:gson:2.13.2")
}

//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.compose.ui)
//    implementation(libs.androidx.compose.ui.graphics)
//    implementation(libs.androidx.compose.ui.tooling.preview)
//    implementation(libs.androidx.compose.material3)
//
//    implementation("androidx.compose.foundation:foundation")
//
//    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
//    // Firebase Authentication
//    implementation("com.google.firebase:firebase-auth-ktx:23.2.1") //
//    implementation("com.google.firebase:firebase-storage")
//    // Firestore Database
//    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
//    implementation(libs.firebase.auth)
//    implementation(libs.androidx.credentials)
//    implementation(libs.androidx.credentials.play.services.auth)
//    implementation(libs.googleid)//
//    implementation("com.google.android.gms:play-services-auth:21.4.0")
//
//    implementation("androidx.compose.material:material-icons-extended")
//
//    //do kamery
//    implementation("androidx.camera:camera-camera2:1.5.2")
//    implementation("androidx.camera:camera-lifecycle:1.5.2")
//    implementation("androidx.camera:camera-view:1.5.2")
//    implementation("androidx.camera:camera-core:1.5.2")
//    implementation("com.google.guava:guava:31.1-android")
//    implementation(libs.firebase.storage.ktx)
//    // chat mówi że nie korzystać z wersji bo firbase BoM ją ma i reszta niby automatycznie się dobiera ale się wywala bez nich, narazie zostawiam tak
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
//    debugImplementation(libs.androidx.compose.ui.tooling)
//    debugImplementation(libs.androidx.compose.ui.test.manifest)
//    implementation("io.coil-kt:coil-compose:2.7.0")
//}