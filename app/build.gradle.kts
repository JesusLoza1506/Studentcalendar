plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
}

val releaseKeystore = providers.environmentVariable("KALENDU_KEYSTORE").orNull

android {
    namespace = "com.example.studentcalendar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.studentcalendar"
        minSdk = 23
        targetSdk = 35
        versionCode = providers.environmentVariable("KALENDU_VERSION_CODE").orNull?.toInt() ?: 1
        versionName = providers.environmentVariable("KALENDU_VERSION_NAME").orNull ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("kalenduRelease") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("KALENDU_STORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("KALENDU_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("KALENDU_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("kalenduRelease")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Android KTX (Extensiones de Kotlin)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Interfaz de Usuario y Diseño
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.gridlayout)
    implementation ("com.google.android.flexbox:flexbox:3.0.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.android.gms:play-services-auth:21.0.0") //Inicio de sesión con Google

    // Carga de Imágenes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Pruebas
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}



