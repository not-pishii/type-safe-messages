plugins {
    alias(libs.plugins.vanniktech.maven.publish)
}

mavenPublishing {
    publishToMavenCentral()

    coordinates("me.supcheg", project.name, version.toString())

    pom {
        name = project.name
        description = "Type-safe localized message formatting for Java"
        url = "https://github.com/not-pishii/type-safe-messages"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "not-pishii"
                name = "Egor Pishii"
                url = "https://github.com/not-pishii"
            }
        }
        scm {
            url = "https://github.com/not-pishii/type-safe-messages"
            connection = "scm:git:git://github.com/not-pishii/type-safe-messages.git"
            developerConnection = "scm:git:ssh://git@github.com/not-pishii/type-safe-messages.git"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "testRepo"
            url = uri(rootProject.layout.buildDirectory.dir("test-repo"))
        }
    }
}
