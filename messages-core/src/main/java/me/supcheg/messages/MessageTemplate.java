package me.supcheg.messages;

import java.util.List;
import java.util.function.Function;

/** Распарсенный шаблон сообщения. Иммутабелен. */
public record MessageTemplate(String key, List<TemplatePart> parts) {

    public MessageTemplate {
        parts = List.copyOf(parts);
    }

    /** Тотальный fold: не бросает исключений, если {@code arguments} и {@code renderer} тотальны. */
    public <T> T render(MessageRenderer<T> renderer, Function<String, Object> arguments) {
        List<T> rendered = parts.stream()
            .map(part -> switch (part) {
                case Literal(String text) -> renderer.literal(text);
                case Placeholder(String name) -> renderer.argument(arguments.apply(name));
            })
            .toList();
        return renderer.concat(rendered);
    }
}
