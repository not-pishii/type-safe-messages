plugins {
    alias(conventions.plugins.messages.java.conventions)
    alias(conventions.plugins.messages.bundle.conventions)
}

dependencies {
    api(project(":example:example-contract"))
    annotationProcessor(project(":messages-processor"))
}
