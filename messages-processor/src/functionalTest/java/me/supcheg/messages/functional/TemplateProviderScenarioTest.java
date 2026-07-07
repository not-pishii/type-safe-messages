package me.supcheg.messages.functional;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateProviderScenarioTest {

    @Test
    void customProviderModuleSuppliesTranslationsAndRebuildsOnResourceChange(@TempDir Path tmp) {
        TestProject project = new TestProject(tmp).withJsonTranslationsProvider();
        project.write("settings.gradle.kts", """
            dependencyResolutionManagement {
                repositories {
                    maven(url = "%s")
                    mavenCentral()
                }
            }
            include("contract", "translations", "bundle", "app")
            """.formatted(TestProject.REPO));
        project.write("bundle/build.gradle.kts", """
            plugins { `java-library` }
            dependencies {
                api(project(":contract"))
                annotationProcessor("me.supcheg:messages-processor:%s")
                annotationProcessor(project(":translations"))
                compileOnly(project(":translations"))
            }
            """.formatted(TestProject.VERSION));
        project.write("bundle/src/main/java/msg/GameBundle.java", """
            package msg;

            import me.supcheg.messages.annotation.MessageBundle;
            import translations.JsonTemplateProvider;

            @MessageBundle(contract = GameMessages.class, locales = {"ru"}, provider = JsonTemplateProvider.class)
            final class GameBundle {
            }
            """);

        BuildResult first = GradleRunner.create()
                .withProjectDir(project.root().toFile())
                .withArguments("run", "-q")
                .build();
        assertThat(first.getOutput()).contains("Игрок Steve зашёл");

        project.write("translations/src/main/resources/translations/messages_ru.json", """
            {
              "playerJoined": "Здравствуй, {player}",
              "balance": "У {player} на счету {coins}"
            }
            """);

        BuildResult second = GradleRunner.create()
                .withProjectDir(project.root().toFile())
                .withArguments("run", "-q")
                .build();
        assertThat(second.getOutput()).contains("Здравствуй, Steve");
    }

    @Test
    void providerMissingFromAnnotationProcessorClasspathFailsWithActionableMessage(@TempDir Path tmp) {
        TestProject project = new TestProject(tmp).withJsonTranslationsProvider();
        project.write("settings.gradle.kts", """
            dependencyResolutionManagement {
                repositories {
                    maven(url = "%s")
                    mavenCentral()
                }
            }
            include("contract", "translations", "bundle", "app")
            """.formatted(TestProject.REPO));
        project.write("bundle/build.gradle.kts", """
            plugins { `java-library` }
            dependencies {
                api(project(":contract"))
                annotationProcessor("me.supcheg:messages-processor:%s")
                compileOnly(project(":translations"))
            }
            """.formatted(TestProject.VERSION));
        project.write("bundle/src/main/java/msg/GameBundle.java", """
            package msg;

            import me.supcheg.messages.annotation.MessageBundle;
            import translations.JsonTemplateProvider;

            @MessageBundle(contract = GameMessages.class, locales = {"ru"}, provider = JsonTemplateProvider.class)
            final class GameBundle {
            }
            """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(project.root().toFile())
                .withArguments("compileJava")
                .buildAndFail();
        assertThat(result.getOutput()).contains("annotationProcessor");
    }
}
