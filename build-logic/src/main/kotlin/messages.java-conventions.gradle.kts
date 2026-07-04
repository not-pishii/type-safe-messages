import gradle.kotlin.dsl.accessors._285c6d39ba46e7d28bd8eed795cb183a.spotless
import org.gradle.internal.impldep.org.eclipse.jgit.util.RawCharUtil.trimTrailingWhitespace

plugins {
    `java-library`
    alias(libs.plugins.spotless)
}

group = "me.supcheg"

version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing"))
    }
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(libs.assertj.core)
            }
        }
    }
}

spotless {
    java {
        palantirJavaFormat()

        importOrder("", "javax|java", "\\#")
        forbidWildcardImports()

        targetExclude("build/**")
    }

    kotlinGradle {
        ktfmt().kotlinlangStyle()

        trimTrailingWhitespace()
        endWithNewline()

        targetExclude("build/**")
    }
}
