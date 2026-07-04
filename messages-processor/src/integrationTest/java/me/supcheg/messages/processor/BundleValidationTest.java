package me.supcheg.messages.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class BundleValidationTest {

    private static final String CONTRACT = """
        package com.example;

        import me.supcheg.messages.annotation.Messages;

        @Messages
        public interface GameMessages<T> {
            T playerJoined(String player);
            T balance(String player, int coins);
        }
        """;

    private static Compilation compileWithFixture(String fixture, String bundleSource) {
        Path dir = Path.of("src", "integrationTest", "resources", "fixtures", fixture).toAbsolutePath();
        return javac()
            .withProcessors(new MessagesProcessor())
            .withOptions("-Amessages.dir=" + dir)
            .compile(
                JavaFileObjects.forSourceString("com.example.GameMessages", CONTRACT),
                JavaFileObjects.forSourceString("com.example.GameBundle", bundleSource));
    }

    private static final String BUNDLE_RU_EN = """
        package com.example;

        import me.supcheg.messages.annotation.MessageBundle;

        @MessageBundle(contract = GameMessages.class, locales = {"ru", "en"})
        final class GameBundle {
        }
        """;

    private static final String BUNDLE_RU = """
        package com.example;

        import me.supcheg.messages.annotation.MessageBundle;

        @MessageBundle(contract = GameMessages.class, locales = {"ru"})
        final class GameBundle {
        }
        """;

    @Test
    void validBundleCompiles() {
        assertThat(compileWithFixture("valid", BUNDLE_RU_EN)).succeeded();
    }

    @Test
    void missingKeyFailsWithLocaleAndKey() {
        Compilation compilation = compileWithFixture("missing-key", BUNDLE_RU);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("balance");
        assertThat(compilation).hadErrorContaining("[ru]");
    }

    @Test
    void typoPlaceholderFailsWithExpectedNames() {
        Compilation compilation = compileWithFixture("typo-placeholder", BUNDLE_RU);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("playr");
        assertThat(compilation).hadErrorContaining("player");
    }

    @Test
    void unusedParameterAndExtraKeyAreWarningsNotErrors() {
        Compilation compilation = compileWithFixture("warnings", BUNDLE_RU);

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("parameter 'player' is not used");
        assertThat(compilation).hadWarningContaining("'extra.key' is not declared in the contract");
    }

    @Test
    void missingDirOptionFails() {
        Compilation compilation = javac()
            .withProcessors(new MessagesProcessor())
            .compile(
                JavaFileObjects.forSourceString("com.example.GameMessages", CONTRACT),
                JavaFileObjects.forSourceString("com.example.GameBundle", BUNDLE_RU));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("messages.dir");
    }
}
