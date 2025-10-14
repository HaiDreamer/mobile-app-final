plugins {
    alias(libs.plugins.android.application)
    // Add Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "vn.edu.usth.ircui"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.edu.usth.ircui"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }
}

dependencies {
    // import Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    // Firestore
    implementation("com.google.firebase:firebase-firestore")


    implementation("org.kitteh.irc:client-lib:9.0.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)

    // 🟢 Add these new dependencies for the DM UI
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment:1.8.2")
    implementation("androidx.activity:activity:1.9.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
