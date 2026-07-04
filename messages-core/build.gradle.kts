plugins {
    id("messages.java-conventions")
    id("messages.publishing")
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
