package me.supcheg.messages.example;

import me.supcheg.messages.MessageRenderer;

import java.util.Locale;
import java.util.Map;

/**
 * Discovered via {@link java.util.ServiceLoader} so {@code example-app} can render {@link
 * GameMessages} for every locale a bundle provides without a compile-time dependency on any
 * specific bundle module — exactly one matrix module is put on the runtime classpath at a time.
 */
public interface GameMessagesSource {

    Map<Locale, GameMessages<String>> load(MessageRenderer<String> renderer);
}
