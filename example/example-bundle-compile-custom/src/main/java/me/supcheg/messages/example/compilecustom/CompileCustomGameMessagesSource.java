package me.supcheg.messages.example.compilecustom;

import me.supcheg.messages.MessageRenderer;
import me.supcheg.messages.example.GameMessages;
import me.supcheg.messages.example.GameMessagesSource;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CompileCustomGameMessagesSource implements GameMessagesSource {

    @Override
    public Map<Locale, GameMessages<String>> load(MessageRenderer<String> renderer) {
        Map<Locale, GameMessages<String>> byLocale = new LinkedHashMap<>();
        for (Locale locale : GameMessagesBundle.locales()) {
            GameMessagesBundle.forLocale(locale, renderer).ifPresent(messages -> byLocale.put(locale, messages));
        }
        return byLocale;
    }
}
