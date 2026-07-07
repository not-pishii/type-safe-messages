package me.supcheg.messages.processor.providers;

import me.supcheg.messages.spi.SourceProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Real, compiled-onto-the-test-classpath {@link TemplateProvider} used by
 * {@link me.supcheg.messages.processor.TemplateProviderGenerationTest} to exercise the processor's
 * reflective loading path. Must be a genuine {@code .class} file reachable via
 * {@code MessagesProcessor.class.getClassLoader()} (i.e. compiled normally, not via
 * {@code JavaFileObjects.forSourceString} in-memory compilation) since that is exactly what the
 * production {@code Class.forName} call requires.
 */
public final class InMemoryProvider implements TemplateProvider {
    @Override
    public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
        return Either.right(Map.of("playerJoined", "CUSTOM PROVIDER OUTPUT"));
    }
}
