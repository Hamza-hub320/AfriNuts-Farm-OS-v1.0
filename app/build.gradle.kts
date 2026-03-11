plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.afrinuts.farmos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.afrinuts.farmos"
        minSdk = 24
        targetSdk = 34
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Test options for JUnit 4
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Android Core
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.7.0")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Networking (Weather API)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Unit Testing - JUnit 4
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.json:json:20230227")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Android Instrumentation Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")

    // Material Icons (Extended for more icon options)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // PDF Generation
    implementation("com.itextpdf:itext7-core:7.2.5")

    // For file sharing
    implementation("androidx.core:core:1.12.0")

    // Firebase BOM (manages versions automatically)
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))

// Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

// Cloud Firestore
    implementation("com.google.firebase:firebase-firestore")

// Firebase Storage (for photos)
    implementation("com.google.firebase:firebase-storage")

// Firebase Cloud Messaging (push notifications)
    implementation("com.google.firebase:firebase-messaging")

// Firebase Analytics
    implementation("com.google.firebase:firebase-analytics")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime:2.9.0")
}