plugins {
    java
}

val messagesDir = layout.projectDirectory.dir("src/main/messages")

tasks {
    compileJava {
        val messagesDir = layout.projectDirectory.dir("src/main/messages")

        inputs.dir(messagesDir)
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .withPropertyName("messagesDir")

        options.compilerArgs.add("-Amessages.dir=${messagesDir.asFile.absolutePath}")
    }
}
