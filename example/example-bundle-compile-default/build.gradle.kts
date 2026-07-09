plugins {
    alias(conventions.plugins.messages.java.conventions)
    alias(conventions.plugins.messages.bundle.conventions)
}

dependencies {
    api(project(":example:messages-declaration"))
    annotationProcessor(project(":messages-processor"))
}
