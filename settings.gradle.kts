pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "huntdex"

include(":app")
include(":desktopApp")
include(":core:domain")
include(":core:data")
include(":core:navigation")
include(":core:ui")
include(":core:common")
include(":feature:pokedex")
include(":feature:moves")
include(":feature:items")
include(":feature:locations")
include(":feature:games")
include(":feature:hunting")
include(":feature:profile")
