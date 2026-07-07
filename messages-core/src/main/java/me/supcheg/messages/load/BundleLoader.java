package me.supcheg.messages.load;

import me.supcheg.messages.MessageTemplate;
import me.supcheg.messages.Placeholder;
import me.supcheg.messages.parse.ParseResult;
import me.supcheg.messages.parse.TemplateParser;
import me.supcheg.messages.spi.PathResourceOpener;
import me.supcheg.messages.spi.PropertiesProvider;
import me.supcheg.messages.spi.SourceProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads and validates translations for a locale from any {@link TemplateProvider}, parsing raw
 * templates and checking placeholders against a {@link ContractShape} identically for every
 * source.
 */
public final class BundleLoader {

    private BundleLoader() {}

    public static Either<List<ContentProblem>, Map<String, MessageTemplate>> load(
            Path dir, Locale locale, String baseName, ContractShape shape) {
        return load(new PropertiesProvider(baseName, new PathResourceOpener(dir)), locale, shape);
    }

    public static Either<List<ContentProblem>, Map<String, MessageTemplate>> load(
            TemplateProvider provider, Locale locale, ContractShape shape) {
        return provider.templates(locale)
                .mapLeft(sourceProblems -> sourceProblems.stream()
                        .map(BundleLoader::asContentProblem)
                        .toList())
                .flatMapRight(raw -> parseAndValidate(raw, locale, shape));
    }

    private static ContentProblem asContentProblem(SourceProblem sp) {
        return new ContentProblem.SourceProblem(sp.locale(), sp.description());
    }

    private static Either<List<ContentProblem>, Map<String, MessageTemplate>> parseAndValidate(
            Map<String, String> raw, Locale locale, ContractShape shape) {
        List<ContentProblem> problems = new ArrayList<>();
        Map<String, MessageTemplate> content = new HashMap<>();
        for (MessageShape message : shape.messages()) {
            String rawTemplate = raw.get(message.key());
            if (rawTemplate == null) {
                problems.add(new ContentProblem.MissingKey(locale, message.key()));
                continue;
            }
            switch (TemplateParser.parse(message.key(), rawTemplate)) {
                case ParseResult.Invalid(String key, int position, String reason) ->
                    problems.add(new ContentProblem.MalformedTemplate(locale, key, position, reason));
                case ParseResult.Parsed(MessageTemplate template) -> {
                    Set<String> expected = Set.copyOf(message.placeholders());
                    Set<String> unknown = template.parts().stream()
                            .filter(part -> part instanceof Placeholder)
                            .map(part -> ((Placeholder) part).name())
                            .filter(name -> !expected.contains(name))
                            .collect(Collectors.toSet());
                    if (unknown.isEmpty()) {
                        content.put(message.key(), template);
                    } else {
                        unknown.forEach(name -> problems.add(
                                new ContentProblem.UnknownPlaceholder(locale, message.key(), name, expected)));
                    }
                }
            }
        }
        return problems.isEmpty() ? Either.right(Map.copyOf(content)) : Either.left(List.copyOf(problems));
    }
}
