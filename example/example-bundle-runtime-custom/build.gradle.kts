plugins {
    alias(conventions.plugins.messages.java.conventions)
}

dependencies {
    api(project(":example:messages-declaration"))
    annotationProcessor(project(":messages-processor"))
    annotationProcessor(project(":example:example-translations"))
    implementation(project(":example:example-translations"))
}
