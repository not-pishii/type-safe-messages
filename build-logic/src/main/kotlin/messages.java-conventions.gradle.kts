plugins {
    `java-library`
    alias(libs.plugins.spotless)
}

group = "me.supcheg"

version = "1.2.0"

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
