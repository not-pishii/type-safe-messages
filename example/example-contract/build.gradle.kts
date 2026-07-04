plugins {
    id("messages.java-conventions")
}

dependencies {
    api(project(":messages-annotations"))
    api(project(":messages-core"))
    annotationProcessor(project(":messages-processor"))
}
