plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(17)
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(projects.core.navigation)
                implementation(projects.core.data)
                implementation(libs.koin.core)
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.koin)
                implementation(libs.coroutines.core)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.huntdex.desktopapp.MainKt"
    }
}
