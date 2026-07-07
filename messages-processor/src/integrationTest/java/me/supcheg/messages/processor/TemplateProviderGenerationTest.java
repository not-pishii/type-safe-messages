package me.supcheg.messages.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Exercises the processor's custom-{@code TemplateProvider} path end-to-end.
 *
 * <p>The providers under test ({@code me.supcheg.messages.processor.providers.*}) are real,
 * separately-compiled classes living in this module's {@code integrationTest} source set — NOT
 * {@link JavaFileObjects#forSourceString} in-memory sources compiled alongside the bundle under
 * test. This matters: {@code compile-testing} compiles sources passed to {@code compile(...)}
 * entirely in-memory, with no {@code .class} file ever produced on any real classpath, so
 * {@code Class.forName(fqn, true, MessagesProcessor.class.getClassLoader())} — the exact
 * production code path — could never find them. Real usage always has the provider already
 * compiled onto the annotation processor's classpath (e.g. via a separate module added to the
 * {@code annotationProcessor} configuration); these tests reproduce that by using providers
 * compiled as part of this test module itself, which therefore *are* reachable from
 * {@code MessagesProcessor.class.getClassLoader()} since it's the same test JVM.
 */
class TemplateProviderGenerationTest {

    private static final String CONTRACT = """
        package com.example;

        import me.supcheg.messages.annotation.Messages;

        @Messages
        public interface GameMessages<T> {
            T playerJoined(String player);
        }
        """;

    private static final Path VALID_FIXTURE_DIR =
            Path.of("src", "integrationTest", "resources", "fixtures", "valid").toAbsolutePath();

    private static String bundleSource(String providerSimpleName) {
        return """
            package com.example;

            import me.supcheg.messages.annotation.MessageBundle;
            import me.supcheg.messages.processor.providers.%s;

            @MessageBundle(contract = GameMessages.class, locales = {"ru"}, provider = %s.class)
            final class GameBundle {
            }
            """.formatted(providerSimpleName, providerSimpleName);
    }

    private static Compilation compile(String providerSimpleName) {
        return javac().withProcessors(new MessagesProcessor())
                .withOptions("-Amessages.dir=" + VALID_FIXTURE_DIR)
                .compile(
                        JavaFileObjects.forSourceString("com.example.GameMessages", CONTRACT),
                        JavaFileObjects.forSourceString("com.example.GameBundle", bundleSource(providerSimpleName)));
    }

    @Test
    void compileTimeBundleUsesCustomProviderOutput() {
        Compilation compilation = compile("InMemoryProvider");

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("com.example.GameMessagesBundle")
                .contentsAsUtf8String()
                .contains("new Literal(\"CUSTOM PROVIDER OUTPUT\")");
    }

    @Test
    void providerThrowingDuringTemplatesProducesErrorWithoutCrashingCompilation() {
        Compilation compilation = compile("ThrowingProvider");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("boom");
    }

    @Test
    void providerThrowingDuringConstructionProducesError() {
        Compilation compilation = compile("ConstructorThrowingProvider");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("construction boom");
    }

    @Test
    void abstractProviderFailsValidation() {
        Compilation compilation = compile("AbstractProvider");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must not be abstract");
    }

    @Test
    void providerWithoutPublicNoArgConstructorFailsValidation() {
        Compilation compilation = compile("NoNoArgProvider");

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("public no-arg constructor");
    }

    @Test
    void customResourcesTogetherWithCustomProviderFailsValidation() {
        String bundle = """
            package com.example;

            import me.supcheg.messages.annotation.MessageBundle;
            import me.supcheg.messages.processor.providers.InMemoryProvider;

            @MessageBundle(
                    contract = GameMessages.class,
                    locales = {"ru"},
                    provider = InMemoryProvider.class,
                    resources = "custom")
            final class GameBundle {
            }
            """;
        Compilation compilation = javac().withProcessors(new MessagesProcessor())
                .withOptions("-Amessages.dir=" + VALID_FIXTURE_DIR)
                .compile(
                        JavaFileObjects.forSourceString("com.example.GameMessages", CONTRACT),
                        JavaFileObjects.forSourceString("com.example.GameBundle", bundle));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("has no effect together with a custom provider()");
    }

    @Test
    void unresolvableProviderClassProducesHelpfulErrorSuggestingAnnotationProcessorClasspath() {
        String bundle = """
            package com.example;

            import me.supcheg.messages.annotation.MessageBundle;

            @MessageBundle(contract = GameMessages.class, locales = {"ru"}, provider = NotOnClasspathProvider.class)
            final class GameBundle {
            }
            """;
        String providerDeclaredButNotOnRealClasspath = """
            package com.example;

            import me.supcheg.messages.spi.SourceProblem;
            import me.supcheg.messages.spi.TemplateProvider;
            import me.supcheg.routine.Either;

            import java.util.List;
            import java.util.Locale;
            import java.util.Map;

            public final class NotOnClasspathProvider implements TemplateProvider {
                @Override
                public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
                    return Either.right(Map.of("playerJoined", "unused"));
                }
            }
            """;
        // Compiled together with the bundle in-memory only: resolvable as a TypeElement at
        // annotation-processing time (so validation passes), but never produced as a real .class
        // file reachable from MessagesProcessor.class.getClassLoader() — exactly the situation a
        // user hits when they forget to add the provider's module to `annotationProcessor`.
        Compilation compilation = javac().withProcessors(new MessagesProcessor())
                .withOptions("-Amessages.dir=" + VALID_FIXTURE_DIR)
                .compile(
                        JavaFileObjects.forSourceString("com.example.GameMessages", CONTRACT),
                        JavaFileObjects.forSourceString(
                                "com.example.NotOnClasspathProvider", providerDeclaredButNotOnRealClasspath),
                        JavaFileObjects.forSourceString("com.example.GameBundle", bundle));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("not found on the annotation processor's classpath");
        assertThat(compilation).hadErrorContaining("annotationProcessor");
    }
}
