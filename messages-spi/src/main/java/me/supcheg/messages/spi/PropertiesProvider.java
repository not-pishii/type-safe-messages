package me.supcheg.messages.spi;

import me.supcheg.routine.Either;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Default {@link TemplateProvider}: reads {@code <baseName>_<tag>.properties} through a
 * {@link ResourceOpener}. The single point in the library that reads {@code .properties}.
 */
public final class PropertiesProvider implements TemplateProvider {

    private final String baseName;
    private final ResourceOpener opener;

    public PropertiesProvider(String baseName, ResourceOpener opener) {
        this.baseName = baseName;
        this.opener = opener;
    }

    @Override
    public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
        String fileName = baseName + "_" + locale.toLanguageTag().replace('-', '_') + ".properties";
        Optional<Reader> reader;
        try {
            reader = opener.open(fileName);
        } catch (UncheckedIOException e) {
            return Either.left(List.of(new SourceProblem(
                    locale, "cannot read " + fileName + ": " + e.getCause().getMessage())));
        }
        if (reader.isEmpty()) {
            return Either.left(List.of(new SourceProblem(locale, "resource not found: " + fileName)));
        }
        Properties properties = new Properties();
        try (Reader r = reader.get()) {
            properties.load(r);
        } catch (IOException e) {
            return Either.left(List.of(new SourceProblem(locale, "cannot read " + fileName + ": " + e.getMessage())));
        }
        Map<String, String> raw = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            raw.put(key, properties.getProperty(key));
        }
        return Either.right(Map.copyOf(raw));
    }
}
