package me.supcheg.messages.spi;

import me.supcheg.routine.Either;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Supplies raw, unparsed message templates (with {@code {placeholder}} syntax intact) for a
 * single locale. Implementations return an immutable snapshot keyed by message key; parsing the
 * templates and validating them against the contract's shape always happens in the library,
 * identically for compile-time and runtime resolution.
 */
public interface TemplateProvider {

    Either<List<SourceProblem>, Map<String, String>> templates(Locale locale);
}
