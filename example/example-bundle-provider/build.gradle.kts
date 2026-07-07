plugins {
    alias(conventions.plugins.messages.java.conventions)
}

dependencies {
    api(project(":example:example-contract"))
    annotationProcessor(project(":messages-processor"))
    annotationProcessor(project(":example:example-translations"))
    compileOnly(project(":example:example-translations"))
}
