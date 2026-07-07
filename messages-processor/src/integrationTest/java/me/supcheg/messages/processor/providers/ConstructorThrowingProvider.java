package me.supcheg.messages.processor.providers;

import me.supcheg.messages.spi.SourceProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Throws from its public no-arg constructor to exercise the processor's construction isolation. */
public final class ConstructorThrowingProvider implements TemplateProvider {

    public ConstructorThrowingProvider() {
        throw new IllegalStateException("construction boom");
    }

    @Override
    public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
        return Either.right(Map.of("playerJoined", "unused"));
    }
}
