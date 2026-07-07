package me.supcheg.messages.processor.providers;

import me.supcheg.messages.spi.SourceProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Only has a one-arg constructor; the processor must reject this before ever attempting reflection. */
public final class NoNoArgProvider implements TemplateProvider {

    public NoNoArgProvider(String unused) {}

    @Override
    public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
        return Either.right(Map.of("playerJoined", "unused"));
    }
}
