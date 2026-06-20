plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.huntdex.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.huntdex.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.common)
    implementation(projects.feature.pokedex)
    implementation(projects.feature.moves)

    implementation(libs.voyager.navigator)
    implementation(libs.voyager.koin)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.androidx.activity.compose)
    implementation(compose.runtime)
    implementation(compose.material3)
}
