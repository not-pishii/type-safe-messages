package me.supcheg.messages.functional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Генерирует минимальный мульти-модульный проект contract+bundle+app во временном каталоге. */
final class TestProject {

    static final String REPO = System.getProperty("test.repo");
    static final String VERSION = System.getProperty("test.version");
    static final String ROUTINE_VERSION = "1.0.0";

    private final Path root;

    TestProject(Path root) {
        this.root = root;
        write("settings.gradle.kts", """
            dependencyResolutionManagement {
                repositories {
                    maven(url = "%s")
                    mavenCentral()
                }
            }
            include("contract", "bundle", "app")
            """.formatted(REPO));
        write("contract/build.gradle.kts", """
            plugins { `java-library` }
            dependencies {
                api("me.supcheg:messages-annotations:%1$s")
                api("me.supcheg:messages-core:%1$s")
                annotationProcessor("me.supcheg:messages-processor:%1$s")
            }
            """.formatted(VERSION));
        write("bundle/build.gradle.kts", """
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
            """.formatted(VERSION));
        write("app/build.gradle.kts", """
            plugins { java; application }
            dependencies { implementation(project(":bundle")) }
            application { mainClass = "app.Main" }
            """);
        write("contract/src/main/java/msg/GameMessages.java", """
            package msg;

            import me.supcheg.messages.annotation.Messages;

            @Messages
            public interface GameMessages<T> {
                T playerJoined(String player);
                T balance(String player, int coins);
            }
            """);
        write("bundle/src/main/java/msg/GameBundle.java", """
            package msg;

            import me.supcheg.messages.annotation.MessageBundle;

            @MessageBundle(contract = GameMessages.class, locales = {"ru"})
            final class GameBundle {
            }
            """);
        write("bundle/src/main/messages/messages_ru.properties", """
            playerJoined=Игрок {player} зашёл
            balance=У {player} на счету {coins}
            """);
        write("app/src/main/java/app/Main.java", """
            package app;

            import me.supcheg.messages.StringRenderer;
            import msg.GameMessagesBundle;

            public final class Main {
                public static void main(String[] args) {
                    System.out.println(GameMessagesBundle.ru(StringRenderer.instance()).playerJoined("Steve"));
                }
            }
            """);
    }

    TestProject withJsonTranslationsProvider() {
        write("translations/build.gradle.kts", """
            plugins { `java-library` }
            dependencies {
                implementation("me.supcheg:messages-spi:%s")
                implementation("me.supcheg:routine:%s")
            }
            """.formatted(VERSION, ROUTINE_VERSION));
        write("translations/src/main/java/translations/JsonTemplateProvider.java", """
            package translations;

            import me.supcheg.messages.spi.SourceProblem;
            import me.supcheg.messages.spi.TemplateProvider;
            import me.supcheg.routine.Either;

            import java.io.IOException;
            import java.io.InputStream;
            import java.io.UncheckedIOException;
            import java.nio.charset.StandardCharsets;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Locale;
            import java.util.Map;
            import java.util.regex.Matcher;
            import java.util.regex.Pattern;

            /** Minimal hand-rolled JSON object reader ("key": "value" pairs only) -- enough to
             *  prove a non-properties source works; not a general JSON parser. */
            public final class JsonTemplateProvider implements TemplateProvider {

                private static final Pattern ENTRY = Pattern.compile("\\"(.+?)\\"\\s*:\\s*\\"(.+?)\\"");

                @Override
                public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
                    String fileName = "translations/messages_" + locale.toLanguageTag().replace('-', '_') + ".json";
                    try (InputStream in = JsonTemplateProvider.class.getClassLoader().getResourceAsStream(fileName)) {
                        if (in == null) {
                            return Either.left(List.of(new SourceProblem(locale, "classpath:/" + fileName + " not found")));
                        }
                        String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, String> result = new HashMap<>();
                        Matcher m = ENTRY.matcher(json);
                        while (m.find()) {
                            result.put(m.group(1), m.group(2));
                        }
                        return Either.right(Map.copyOf(result));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
            """);
        write("translations/src/main/resources/translations/messages_ru.json", """
            {
              "playerJoined": "Игрок {player} зашёл",
              "balance": "У {player} на счету {coins}"
            }
            """);
        return this;
    }

    TestProject write(String relativePath, String content) {
        try {
            Path file = root.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    TestProject delete(String relativePath) {
        try {
            Files.delete(root.resolve(relativePath));
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Path root() {
        return root;
    }
}
