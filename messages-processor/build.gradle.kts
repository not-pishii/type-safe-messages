plugins {
    alias(conventions.plugins.messages.java.conventions)
    alias(conventions.plugins.messages.test.suites)
    alias(conventions.plugins.messages.publishing)
}

dependencies {
    implementation(project(":messages-annotations"))
    implementation(project(":messages-core"))
}

testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(libs.compile.testing)
                implementation(project(":messages-annotations"))
                implementation(project(":messages-core"))
            }
        }
        named<JvmTestSuite>("functionalTest") {
            dependencies {
                implementation(gradleTestKit())
            }
        }
    }
}

tasks {
    functionalTest {
        dependsOn(
            ":messages-spi:publishAllPublicationsToTestRepoRepository",
            ":messages-annotations:publishAllPublicationsToTestRepoRepository",
            ":messages-core:publishAllPublicationsToTestRepoRepository",
            ":messages-processor:publishAllPublicationsToTestRepoRepository",
        )
        systemProperty(
            "test.repo",
            rootProject.layout.buildDirectory.dir("test-repo").get().asFile.toURI().toString(),
        )
        systemProperty("test.version", version.toString())
    }
}
