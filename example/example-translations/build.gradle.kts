plugins {
    alias(conventions.plugins.messages.java.conventions)
}

dependencies {
    api(project(":messages-spi"))
}
