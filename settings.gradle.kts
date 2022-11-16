pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "android-sensor"
include(":core")

include(":widgets")
include(":widgets:card")
include(":sample")
