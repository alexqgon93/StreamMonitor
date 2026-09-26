plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val streamDataSource = providers.gradleProperty("streamDataSource").getOrElse("mock")
val mockScenario = providers.gradleProperty("mockScenario").getOrElse("happy_path")
val numbersEndpoint = providers.gradleProperty("numbersEndpoint").getOrElse("")
val inputsEndpoint = providers.gradleProperty("inputsEndpoint").getOrElse("")

android {
    namespace = "com.alexqgon.streammonitor"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.alexqgon.streammonitor"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "STREAM_DATA_SOURCE", "\"$streamDataSource\"")
        buildConfigField("String", "MOCK_SCENARIO", "\"$mockScenario\"")
        buildConfigField("String", "NUMBERS_ENDPOINT", "\"$numbersEndpoint\"")
        buildConfigField("String", "INPUTS_ENDPOINT", "\"$inputsEndpoint\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":feature:stream"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}