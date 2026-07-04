import com.diffplug.gradle.spotless.BaseKotlinExtension

plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

spotless {
    val baseKotlinConfiguration: BaseKotlinExtension.() -> Unit = {
        ktfmt().kotlinlangStyle()

        trimTrailingWhitespace()
        endWithNewline()

        targetExclude("build/**")
    }

    kotlin(baseKotlinConfiguration)
    kotlinGradle(baseKotlinConfiguration)
}
