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
    "messages-spi",
    "messages-annotations",
    "messages-core",
    "messages-processor",
    "example:messages-declaration",
    "example:example-translations",
    "example:example-bundle-compile-default",
    "example:example-bundle-compile-custom",
    "example:example-bundle-runtime-default",
    "example:example-bundle-runtime-custom",
    "example:example-app",
)
