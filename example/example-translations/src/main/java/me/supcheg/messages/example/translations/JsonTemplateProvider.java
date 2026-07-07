package me.supcheg.messages.example.translations;

import me.supcheg.messages.spi.SourceProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A {@link TemplateProvider} that reads translations from a classpath JSON resource instead of a
 * {@code .properties} file, demonstrating the "flagship" classpath-resource pattern for custom
 * translation sources.
 */
public final class JsonTemplateProvider implements TemplateProvider {
    @Override
    public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
        String path = "me/supcheg/messages/example/translations/messages_"
                + locale.toLanguageTag().replace('-', '_') + ".json";
        try (InputStream in = JsonTemplateProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return Either.left(List.of(new SourceProblem(locale, "classpath:/" + path + " not found")));
            }
            var mapper = new ObjectMapper();
            var mapType = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);

            return Either.right(mapper.readValue(in, mapType));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
