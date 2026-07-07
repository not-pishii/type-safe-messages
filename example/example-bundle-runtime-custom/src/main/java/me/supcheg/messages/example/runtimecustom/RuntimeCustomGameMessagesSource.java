package me.supcheg.messages.example.runtimecustom;

import me.supcheg.messages.MessageRenderer;
import me.supcheg.messages.example.GameMessages;
import me.supcheg.messages.example.GameMessagesSource;
import me.supcheg.messages.example.translations.JsonTemplateProvider;
import me.supcheg.messages.load.ContentProblem;
import me.supcheg.routine.EitherCollectors;
import me.supcheg.routine.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class RuntimeCustomGameMessagesSource implements GameMessagesSource {

    private static final List<Locale> LOCALES = List.of(Locale.forLanguageTag("ru"), Locale.forLanguageTag("en"));

    @Override
    public Map<Locale, GameMessages<String>> load(MessageRenderer<String> renderer) {
        var provider = new JsonTemplateProvider();

        var result = LOCALES.stream()
                .map(locale -> GameMessagesRuntimeBundle.load(provider, locale, renderer)
                        .mapRight(messages -> Pair.pair(locale, messages)))
                .collect(EitherCollectors.groupingTo(
                        Collectors.flatMapping(Collection::stream, Collectors.groupingBy(ContentProblem::locale)),
                        Collectors.toUnmodifiableMap(Pair::left, Pair::right)));

        result.left()
                .forEach((locale, problem) -> System.err.printf(
                        "PROBLEM(%s): %s",
                        locale.toLanguageTag(), problem.getFirst().describe()));
        return result.right();
    }
}
