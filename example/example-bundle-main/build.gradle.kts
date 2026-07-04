plugins {
    id("messages.java-conventions")
    id("messages.bundle-conventions")
}

dependencies {
    api(project(":example:example-contract"))
    annotationProcessor(project(":messages-processor"))
}
