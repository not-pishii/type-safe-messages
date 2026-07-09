plugins {
    alias(conventions.plugins.messages.java.conventions)
    application
}

dependencies {
    implementation(project(":example:messages-declaration"))
    testRuntimeOnly(project(":example:example-bundle-compile-default"))
}

application {
    mainClass = "me.supcheg.messages.example.app.Main"
}

// One runtime-only classpath per matrix cell. Each `extendsFrom(runtimeClasspath)` so it still
// carries messages-declaration/messages-core, then adds exactly one matrix module — `app` itself
// never has an `implementation`/`api`/`compileOnly` dependency on any of them.
val compileDefaultRuntimeClasspath =
    configurations.create("compileDefaultRuntimeClasspath") {
        isCanBeConsumed = false
        extendsFrom(configurations.runtimeClasspath.get())
    }
val compileCustomRuntimeClasspath =
    configurations.create("compileCustomRuntimeClasspath") {
        isCanBeConsumed = false
        extendsFrom(configurations.runtimeClasspath.get())
    }
val runtimeDefaultRuntimeClasspath =
    configurations.create("runtimeDefaultRuntimeClasspath") {
        isCanBeConsumed = false
        extendsFrom(configurations.runtimeClasspath.get())
    }
val runtimeCustomRuntimeClasspath =
    configurations.create("runtimeCustomRuntimeClasspath") {
        isCanBeConsumed = false
        extendsFrom(configurations.runtimeClasspath.get())
    }

dependencies {
    add(compileDefaultRuntimeClasspath.name, project(":example:example-bundle-compile-default"))
    add(compileCustomRuntimeClasspath.name, project(":example:example-bundle-compile-custom"))
    add(runtimeDefaultRuntimeClasspath.name, project(":example:example-bundle-runtime-default"))
    add(runtimeCustomRuntimeClasspath.name, project(":example:example-bundle-runtime-custom"))
}

tasks {
    val runCompileDefault =
        register<JavaExec>("runCompileDefault") {
            group = "example matrix"
            description =
                "Runs the app with the COMPILE_TIME/default-provider bundle on the runtime classpath only."
            mainClass = application.mainClass
            classpath = sourceSets.main.get().output + compileDefaultRuntimeClasspath
        }

    val runCompileCustom =
        register<JavaExec>("runCompileCustom") {
            group = "example matrix"
            description =
                "Runs the app with the COMPILE_TIME/custom-provider bundle on the runtime classpath only."
            mainClass = application.mainClass
            classpath = sourceSets.main.get().output + compileCustomRuntimeClasspath
        }

    val runRuntimeDefault =
        register<JavaExec>("runRuntimeDefault") {
            group = "example matrix"
            description =
                "Runs the app with the RUNTIME/default-provider bundle on the runtime classpath only."
            mainClass = application.mainClass
            classpath = sourceSets.main.get().output + runtimeDefaultRuntimeClasspath
        }

    val runRuntimeCustom =
        register<JavaExec>("runRuntimeCustom") {
            group = "example matrix"
            description =
                "Runs the app with the RUNTIME/custom-provider bundle on the runtime classpath only."
            mainClass = application.mainClass
            classpath = sourceSets.main.get().output + runtimeCustomRuntimeClasspath
        }

    register("runMatrix") {
        group = "example matrix"
        description = "Runs bundle variants"
        dependsOn(
            runCompileDefault,
            runCompileCustom,
            runRuntimeDefault,
            runRuntimeCustom,
        )
    }
}
