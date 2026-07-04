package me.supcheg.messages.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class RuntimeGenerationTest {

    @Test
    void generatesRuntimeLoaderBackedBundle() {
        Path dir = Path.of("src", "integrationTest", "resources", "fixtures", "valid").toAbsolutePath();
        Compilation compilation = javac()
            .withProcessors(new MessagesProcessor())
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
                JavaFileObjects.forSourceString("com.example.GameRuntime", """
                    package com.example;

                    import me.supcheg.messages.annotation.MessageBundle;
                    import me.supcheg.messages.annotation.Resolution;

                    @MessageBundle(contract = GameMessages.class, locales = {"ru", "en"},
                        resolution = Resolution.RUNTIME)
                    final class GameRuntime {
                    }
                    """));

        assertThat(compilation).succeeded();
        var contents = assertThat(compilation).generatedSourceFile("com.example.GameMessagesRuntimeBundle")
            .contentsAsUtf8String();
        contents.contains("public static <T> BundleLoad<GameMessages<T>> load(Path dir, Locale locale, MessageRenderer<T> renderer)");
        contents.contains("BundleLoader.load(dir, locale, \"messages\", com.example.GameMessagesContract.SHAPE)");
        contents.contains("content.get(\"playerJoined\").render(renderer, args)");
    }

    @Test
    void generatesRuntimeBundleWhenContractInDifferentPackage() {
        Path dir = Path.of("src", "integrationTest", "resources", "fixtures", "valid").toAbsolutePath();
        Compilation compilation = javac()
            .withProcessors(new MessagesProcessor())
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
                JavaFileObjects.forSourceString("com.example.impl.GameRuntime", """
                    package com.example.impl;

                    import com.example.api.GameMessages;
                    import me.supcheg.messages.annotation.MessageBundle;
                    import me.supcheg.messages.annotation.Resolution;

                    @MessageBundle(contract = GameMessages.class, locales = {"ru", "en"},
                        resolution = Resolution.RUNTIME)
                    final class GameRuntime {
                    }
                    """));

        assertThat(compilation).succeeded();
        var contents = assertThat(compilation)
            .generatedSourceFile("com.example.impl.GameMessagesRuntimeBundle")
            .contentsAsUtf8String();
        contents.contains("com.example.api.GameMessagesContract.SHAPE");
    }
}
