plugins {
    id("messages.java-conventions")
    id("messages.test-suites")
    id("messages.publishing")
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

tasks.named<Test>("functionalTest") {
    dependsOn(
        ":messages-annotations:publishAllPublicationsToTestRepoRepository",
        ":messages-core:publishAllPublicationsToTestRepoRepository",
        ":messages-processor:publishAllPublicationsToTestRepoRepository",
    )
    systemProperty("test.repo", rootProject.layout.buildDirectory.dir("test-repo").get().asFile.toURI().toString())
    systemProperty("test.version", version.toString())
}
