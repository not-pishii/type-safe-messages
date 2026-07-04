plugins {
    alias(conventions.plugins.messages.java.conventions)
    alias(conventions.plugins.messages.publishing)
}

dependencies {
    api(libs.routine)
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
