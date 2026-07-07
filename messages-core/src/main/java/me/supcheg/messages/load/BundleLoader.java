package me.supcheg.messages.load;

import me.supcheg.messages.MessageTemplate;
import me.supcheg.messages.Placeholder;
import me.supcheg.messages.parse.InvalidTemplate;
import me.supcheg.messages.parse.TemplateParser;
import me.supcheg.messages.spi.PathResourceOpener;
import me.supcheg.messages.spi.PropertiesProvider;
import me.supcheg.messages.spi.SourceProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;
import me.supcheg.routine.EitherCollectors;
import me.supcheg.routine.Pair;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

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
                .mapLeft(sourceProblems ->
                        sourceProblems.stream().map(BundleLoader::sourceProblem).toList())
                .flatMapRight(unparsedTemplates -> parse(unparsedTemplates, locale, shape));
    }

    private static Either<List<ContentProblem>, Map<String, MessageTemplate>> parse(
            Map<String, String> unparsedTemplates, Locale locale, ContractShape shape) {

        return shape.messages().stream()
                .map(message -> Optional.ofNullable(unparsedTemplates.get(message.key()))
                        .map(unparsedTemplate -> TemplateParser.parse(message.key(), unparsedTemplate)
                                .mapLeft(invalid -> List.of(malformedTemplate(locale, invalid)))
                                .flatMapRight(template -> {
                                    var validationResult = validate(template, message, locale);
                                    return validationResult.isEmpty()
                                            ? Either.right(template)
                                            : Either.left(validationResult);
                                })
                                .mapRight(template -> Pair.pair(message.key(), template)))
                        .orElseGet(() -> Either.left(List.of(missingKey(locale, message.key())))))
                .collect(Collectors.collectingAndThen(
                        EitherCollectors.groupingTo(
                                Collectors.flatMapping(Collection::stream, Collectors.toUnmodifiableList()),
                                Collectors.toUnmodifiableMap(Pair::left, Pair::right)),
                        pair -> pair.left().isEmpty() ? Either.right(pair.right()) : Either.left(pair.left())));
    }

    private static List<ContentProblem> validate(MessageTemplate template, MessageShape message, Locale locale) {
        var expectedPlaceholders = Set.copyOf(message.placeholders());

        return template.parts().stream()
                .filter(Placeholder.class::isInstance)
                .map(Placeholder.class::cast)
                .map(Placeholder::name)
                .filter(not(expectedPlaceholders::contains))
                .map(name -> unknownPlaceholder(locale, message.key(), name, expectedPlaceholders))
                .toList();
    }

    private static ContentProblem sourceProblem(SourceProblem sourceProblem) {
        return new ContentProblem.SourceProblem(sourceProblem.locale(), sourceProblem.description());
    }

    private static ContentProblem unknownPlaceholder(Locale locale, String key, String name, Set<String> expected) {
        return new ContentProblem.UnknownPlaceholder(locale, key, name, expected);
    }

    private static ContentProblem missingKey(Locale locale, String key) {
        return new ContentProblem.MissingKey(locale, key);
    }

    private static ContentProblem malformedTemplate(Locale locale, InvalidTemplate template) {
        return new ContentProblem.MalformedTemplate(locale, template.key(), template.position(), template.reason());
    }
}
