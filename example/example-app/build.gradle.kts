plugins {
    id("messages.java-conventions")
    application
}

dependencies {
    implementation(project(":example:example-bundle-main"))
}

application {
    mainClass = "me.supcheg.messages.example.app.Main"
}
