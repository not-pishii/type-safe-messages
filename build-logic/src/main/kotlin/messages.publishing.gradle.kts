plugins {
    alias(libs.plugins.vanniktech.maven.publish)
}

mavenPublishing {
    publishToMavenCentral()

    if (gradle.taskGraph.hasTask(tasks.publishToMavenCentral.name)) {
        signAllPublications()
    }

    coordinates("me.supcheg", project.name, version.toString())

    pom {
        name = project.name
        description = "Type-safe localized message formatting for Java"
        url = "https://github.com/Pupcheg/type-safe-messages"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "Pupcheg"
                name = "Supcheg"
                url = "https://github.com/Pupcheg"
            }
        }
        scm {
            url = "https://github.com/Pupcheg/type-safe-messages"
            connection = "scm:git:git://github.com/Pupcheg/type-safe-messages.git"
            developerConnection = "scm:git:ssh://git@github.com/Pupcheg/type-safe-messages.git"
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
