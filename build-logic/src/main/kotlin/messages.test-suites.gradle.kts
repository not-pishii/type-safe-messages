plugins {
    java
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(project())
                implementation(libs.assertj.core)
            }
        }
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(project())
                implementation(libs.assertj.core)
            }
        }
    }
}

tasks {
    check {
        dependsOn(
            testing.suites.named("integrationTest"),
            testing.suites.named("functionalTest"),
        )
    }
}
