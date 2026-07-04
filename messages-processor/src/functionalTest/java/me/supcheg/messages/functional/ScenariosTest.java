package me.supcheg.messages.functional;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ScenariosTest {

    @TempDir
    Path dir;

    private GradleRunner runner(String... args) {
        return GradleRunner.create().withProjectDir(dir.toFile()).withArguments(args);
    }

    @Test
    void scenario2_missingKeyFailsBuildWithLocaleAndKey() {
        new TestProject(dir).write("bundle/src/main/messages/messages_ru.properties", """
            playerJoined=Игрок {player} зашёл
            """);

        BuildResult result = runner(":bundle:compileJava").buildAndFail();

        assertThat(result.getOutput()).contains("[ru]").contains("balance");
    }

    @Test
    void scenario3_typoPlaceholderFailsWithExpectedNames() {
        new TestProject(dir).write("bundle/src/main/messages/messages_ru.properties", """
            playerJoined=Игрок {playr} зашёл
            balance=У {player} на счету {coins}
            """);

        BuildResult result = runner(":bundle:compileJava").buildAndFail();

        assertThat(result.getOutput()).contains("playr").contains("player");
    }

    @Test
    void scenario4_swappingBundleModuleChangesTextsWithoutCodeChanges() {
        var project = new TestProject(dir);
        // альтернативный бандл: тот же контракт, те же FQN, другие тексты
        project.write("settings.gradle.kts", """
            dependencyResolutionManagement {
                repositories {
                    maven(url = "%s")
                    mavenCentral()
                }
            }
            include("contract", "bundle", "bundleAlt", "app")
            """.formatted(TestProject.REPO));
        project.write("bundleAlt/build.gradle.kts", """
            plugins { `java-library` }
            val messagesDir = layout.projectDirectory.dir("src/main/messages")
            tasks.compileJava {
                inputs.dir(messagesDir).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("messagesDir")
                options.compilerArgs.add("-Amessages.dir=${messagesDir.asFile.absolutePath}")
            }
            dependencies {
                api(project(":contract"))
                annotationProcessor("me.supcheg:messages-processor:%s")
            }
            """.formatted(TestProject.VERSION));
        project.write("bundleAlt/src/main/java/msg/GameBundle.java", """
            package msg;

            import me.supcheg.messages.annotation.MessageBundle;

            @MessageBundle(contract = GameMessages.class, locales = {"ru"})
            final class GameBundle {
            }
            """);
        project.write("bundleAlt/src/main/messages/messages_ru.properties", """
            playerJoined=>> {player} присоединился!
            balance=Баланс {player}: {coins}
            """);
        // app переключается на alt одной строкой зависимости
        project.write("app/build.gradle.kts", """
            plugins { java; application }
            dependencies { implementation(project(":bundleAlt")) }
            application { mainClass = "app.Main" }
            tasks.named<JavaExec>("run") { jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8") }
            """);

        BuildResult result = runner(":app:run", "--quiet").build();

        assertThat(result.getOutput()).contains(">> Steve присоединился!");
    }

    @Test
    void scenario5_runtimeModeBrokenExternalFileFailsFast() {
        var project = new TestProject(dir);
        project.write("bundle/src/main/java/msg/GameRuntime.java", """
            package msg;

            import me.supcheg.messages.annotation.MessageBundle;
            import me.supcheg.messages.annotation.Resolution;

            @MessageBundle(contract = GameMessages.class, locales = {"ru"}, resolution = Resolution.RUNTIME)
            final class GameRuntime {
            }
            """);
        project.write("app/src/main/java/app/Main.java", """
            package app;

            import me.supcheg.messages.StringRenderer;
            import me.supcheg.messages.load.BundleLoad;
            import msg.GameMessages;
            import msg.GameMessagesRuntimeBundle;

            import java.nio.file.Path;
            import java.util.Locale;

            public final class Main {
                public static void main(String[] args) {
                    BundleLoad<GameMessages<String>> load = GameMessagesRuntimeBundle.load(
                        Path.of(args[0]), Locale.of("ru"), StringRenderer.instance());
                    switch (load) {
                        case BundleLoad.Loaded<GameMessages<String>> loaded ->
                            System.out.println("LOADED: " + loaded.messages().playerJoined("Steve"));
                        case BundleLoad.Failed<GameMessages<String>> failed ->
                            failed.problems().forEach(p -> System.out.println("PROBLEM: " + p.describe()));
                    }
                }
            }
            """);
        // внешний каталог с битым файлом: пропущен ключ balance
        project.write("external/messages_ru.properties", """
            playerJoined=Игрок {player} зашёл
            """);
        project.write("app/build.gradle.kts", """
            plugins { java; application }
            dependencies { implementation(project(":bundle")) }
            application { mainClass = "app.Main" }
            tasks.named<JavaExec>("run") { args(rootProject.layout.projectDirectory.dir("external").asFile.absolutePath) }
            """);

        BuildResult result = runner(":app:run", "--quiet").build();

        assertThat(result.getOutput()).contains("PROBLEM:").contains("balance");
    }

    @Test
    void scenario6_editingPropertiesInvalidatesCompilationAndUpToDateOtherwise() {
        new TestProject(dir);

        runner(":bundle:compileJava").build();
        BuildResult second = runner(":bundle:compileJava").build();
        assertThat(second.task(":bundle:compileJava").getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);

        new TestProject(dir); // перезапишет файлы теми же значениями — не считается изменением
        // реальное изменение:
        var project = new TestProject(dir);
        project.write("bundle/src/main/messages/messages_ru.properties", """
            playerJoined=Игрок {player} ЗАШЁЛ
            balance=У {player} на счету {coins}
            """);
        BuildResult third = runner(":bundle:compileJava").build();
        assertThat(third.task(":bundle:compileJava").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }
}
