plugins {
    alias(conventions.plugins.messages.java.conventions)
    alias(conventions.plugins.messages.publishing)
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
