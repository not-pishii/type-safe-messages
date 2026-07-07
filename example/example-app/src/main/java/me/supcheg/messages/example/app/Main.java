package me.supcheg.messages.example.app;

import me.supcheg.messages.StringRenderer;
import me.supcheg.messages.example.GameMessages;
import me.supcheg.messages.example.GameMessagesSource;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

public final class Main {

    public static void main(String[] args) {
        printAll(System.out);
    }

    static void printAll(PrintStream out) {
        GameMessagesSource source = ServiceLoader.load(GameMessagesSource.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no " + GameMessagesSource.class.getName()
                        + " on the classpath; put exactly one example-bundle-* module on the runtime classpath"));

        Map<Locale, GameMessages<String>> byLocale = source.load(StringRenderer.instance());
        for (Map.Entry<Locale, GameMessages<String>> entry : byLocale.entrySet()) {
            Locale locale = entry.getKey();
            GameMessages<String> messages = entry.getValue();
            out.println("[" + locale.toLanguageTag() + "] " + messages.playerJoined("Steve"));
            out.println("[" + locale.toLanguageTag() + "] " + messages.balance("Steve", 10));
        }
    }

    private Main() {}
}
