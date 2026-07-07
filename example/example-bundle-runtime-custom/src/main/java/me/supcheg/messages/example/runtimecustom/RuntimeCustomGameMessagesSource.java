package me.supcheg.messages.example.runtimecustom;

import me.supcheg.messages.MessageRenderer;
import me.supcheg.messages.example.GameMessages;
import me.supcheg.messages.example.GameMessagesSource;
import me.supcheg.messages.example.translations.JsonTemplateProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RuntimeCustomGameMessagesSource implements GameMessagesSource {

    private static final List<Locale> LOCALES = List.of(Locale.forLanguageTag("ru"), Locale.forLanguageTag("en"));

    @Override
    public Map<Locale, GameMessages<String>> load(MessageRenderer<String> renderer) {
        var provider = new JsonTemplateProvider();
        Map<Locale, GameMessages<String>> byLocale = new LinkedHashMap<>();
        for (Locale locale : LOCALES) {
            GameMessagesRuntimeBundle.load(provider, locale, renderer)
                    .accept(
                            problems -> problems.forEach(p -> System.err.println("PROBLEM: " + p.describe())),
                            messages -> byLocale.put(locale, messages));
        }
        return byLocale;
    }
}
