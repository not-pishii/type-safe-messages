package me.supcheg.messages.spi;

import java.util.Locale;

/**
 * A problem that prevented a {@link TemplateProvider} from producing a snapshot for a locale
 * (resource not found, I/O failure, malformed source) — distinct from placeholder/key problems,
 * which the library computes once it has the contract shape.
 */
public record SourceProblem(Locale locale, String description) {}
