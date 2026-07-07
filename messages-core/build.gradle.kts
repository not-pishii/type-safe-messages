plugins {
    alias(conventions.plugins.messages.java.conventions)
    alias(conventions.plugins.messages.publishing)
}

dependencies {
    api(project(":messages-spi"))
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            dependencies {
                implementation(libs.jqwik)
            }
        }
    }
}
