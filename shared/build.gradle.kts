plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    kotlin("native.cocoapods")
}

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = "1.0"
        summary = "Huntdex iOS shared framework"
        homepage = "https://github.com/llenes/huntdex"
        ios.deploymentTarget = "16.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "HuntdexKit"
            isStatic = true
        }
    }

    sourceSets {
        iosMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.koin.core)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.koin)
            implementation(libs.coroutines.core)
            implementation(projects.core.domain)
            implementation(projects.core.navigation)
            implementation(projects.core.data)
            implementation(projects.feature.pokedex)
            implementation(projects.feature.moves)
        }
    }
}
