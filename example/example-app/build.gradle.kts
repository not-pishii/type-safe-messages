plugins {
    alias(conventions.plugins.messages.java.conventions)
    application
}

dependencies {
    implementation(project(":example:example-bundle-main"))
    implementation(project(":example:example-bundle-provider"))
}

application {
    mainClass = "me.supcheg.messages.example.app.Main"
}
