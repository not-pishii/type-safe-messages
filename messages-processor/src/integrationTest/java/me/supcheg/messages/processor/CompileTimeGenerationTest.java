package me.supcheg.messages.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class CompileTimeGenerationTest {

    @Test
    void generatesBundleWithPerLocaleFactories() {
        Path dir = Path.of("src", "integrationTest", "resources", "fixtures", "valid")
                .toAbsolutePath();
        Compilation compilation = javac().withProcessors(new MessagesProcessor())
                .withOptions("-Amessages.dir=" + dir)
                .compile(
                        JavaFileObjects.forSourceString("com.example.GameMessages", """
                    package com.example;

                    import me.supcheg.messages.annotation.Messages;

                    @Messages
                    public interface GameMessages<T> {
                        T playerJoined(String player);
                        T balance(String player, int coins);
                    }
                    """),
                        JavaFileObjects.forSourceString("com.example.GameBundle", """
                    package com.example;

                    import me.supcheg.messages.annotation.MessageBundle;

                    @MessageBundle(contract = GameMessages.class, locales = {"ru", "en"})
                    final class GameBundle {
                    }
                    """));

        assertThat(compilation).succeeded();
        var contents = assertThat(compilation)
                .generatedSourceFile("com.example.GameMessagesBundle")
                .contentsAsUtf8String();
        contents.contains("public static <T> GameMessages<T> ru(MessageRenderer<T> renderer)");
        contents.contains("public static <T> GameMessages<T> en(MessageRenderer<T> renderer)");
        contents.contains(
                "public static <T> Optional<GameMessages<T>> forLocale(Locale locale, MessageRenderer<T> renderer)");
        contents.contains("new Literal(\"Игрок \")");
        contents.contains("case \"player\" -> player;");
    }

    @Test
    void generatesBundleWhenContractInDifferentPackage() {
        Path dir = Path.of("src", "integrationTest", "resources", "fixtures", "valid")
                .toAbsolutePath();
        Compilation compilation = javac().withProcessors(new MessagesProcessor())
                .withOptions("-Amessages.dir=" + dir)
                .compile(
                        JavaFileObjects.forSourceString("com.example.api.GameMessages", """
                    package com.example.api;

                    import me.supcheg.messages.annotation.Messages;

                    @Messages
                    public interface GameMessages<T> {
                        T playerJoined(String player);
                        T balance(String player, int coins);
                    }
                    """),
                        JavaFileObjects.forSourceString("com.example.impl.GameBundle", """
                    package com.example.impl;

                    import com.example.api.GameMessages;
                    import me.supcheg.messages.annotation.MessageBundle;

                    @MessageBundle(contract = GameMessages.class, locales = {"ru", "en"})
                    final class GameBundle {
                    }
                    """));

        assertThat(compilation).succeeded();
        var contents = assertThat(compilation)
                .generatedSourceFile("com.example.impl.GameMessagesBundle")
                .contentsAsUtf8String();
        contents.contains("import com.example.api.GameMessages;");
    }
}
