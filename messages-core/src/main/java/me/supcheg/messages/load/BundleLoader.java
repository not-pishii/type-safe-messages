package me.supcheg.messages.load;

import me.supcheg.messages.MessageTemplate;
import me.supcheg.messages.Placeholder;
import me.supcheg.messages.parse.ParseResult;
import me.supcheg.messages.parse.TemplateParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** Загрузка и валидация переводов из properties-файла. Повторяет проверки процессора. */
public final class BundleLoader {

    private BundleLoader() {}

    public static BundleLoad<Map<String, MessageTemplate>> load(
            Path dir, Locale locale, String baseName, ContractShape shape) {
        String fileName = baseName + "_" + locale.toLanguageTag().replace('-', '_') + ".properties";
        Path file = dir.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            return new BundleLoad.Failed<>(List.of(new ContentProblem.MissingFile(locale, file.toString())));
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            return new BundleLoad.Failed<>(
                    List.of(new ContentProblem.UnreadableFile(locale, file.toString(), e.getMessage())));
        }

        List<ContentProblem> problems = new ArrayList<>();
        Map<String, MessageTemplate> content = new HashMap<>();
        for (MessageShape message : shape.messages()) {
            String raw = properties.getProperty(message.key());
            if (raw == null) {
                problems.add(new ContentProblem.MissingKey(locale, message.key()));
                continue;
            }
            switch (TemplateParser.parse(message.key(), raw)) {
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
        return problems.isEmpty()
                ? new BundleLoad.Loaded<>(Map.copyOf(content))
                : new BundleLoad.Failed<>(List.copyOf(problems));
    }
}
