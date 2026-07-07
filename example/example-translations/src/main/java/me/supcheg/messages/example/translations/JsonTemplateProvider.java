package me.supcheg.messages.example.translations;

import me.supcheg.messages.spi.SourceProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link TemplateProvider} that reads translations from a classpath JSON resource instead of a
 * {@code .properties} file, demonstrating the "flagship" classpath-resource pattern for custom
 * translation sources.
 *
 * <p>This is a minimal, hand-rolled reader for flat {@code "key": "value"} JSON objects only —
 * not a general-purpose JSON parser — mirroring the same approach used in this project's
 * functional test suite for the equivalent scenario.
 */
public final class JsonTemplateProvider implements TemplateProvider {

    private static final Pattern ENTRY = Pattern.compile("\"(.+?)\"\\s*:\\s*\"(.+?)\"");

    @Override
    public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
        String path = "me/supcheg/messages/example/translations/messages_"
                + locale.toLanguageTag().replace('-', '_') + ".json";
        try (InputStream in = JsonTemplateProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return Either.left(List.of(new SourceProblem(locale, "classpath:/" + path + " not found")));
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> result = new HashMap<>();
            Matcher matcher = ENTRY.matcher(json);
            while (matcher.find()) {
                result.put(matcher.group(1), matcher.group(2));
            }
            return Either.right(Map.copyOf(result));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
