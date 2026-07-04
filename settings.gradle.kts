pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "type-safe-messages"

include(
    "messages-annotations",
    "messages-core",
    "messages-processor",
    "example:example-contract",
    "example:example-bundle-main",
    "example:example-bundle-alt",
    "example:example-app",
)
