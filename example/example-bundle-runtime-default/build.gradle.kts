plugins {
    alias(conventions.plugins.messages.java.conventions)
    alias(conventions.plugins.messages.bundle.conventions)
}

dependencies {
    api(project(":example:messages-declaration"))
    annotationProcessor(project(":messages-processor"))
}

sourceSets {
    main {
        // reuse the same properties files as both annotation-processor input (compile-time
        // validation) and real runtime classpath resources (actual runtime loading)
        resources.srcDir(layout.projectDirectory.dir("src/main/messages"))
    }
}
