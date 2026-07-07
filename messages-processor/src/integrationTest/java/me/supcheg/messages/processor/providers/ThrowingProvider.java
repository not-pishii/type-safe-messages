package me.supcheg.messages.processor.providers;

import me.supcheg.messages.spi.SourceProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Throws from {@link #templates(Locale)} to exercise the processor's per-call isolation. */
public final class ThrowingProvider implements TemplateProvider {
    @Override
    public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
        throw new IllegalStateException("boom");
    }
}
