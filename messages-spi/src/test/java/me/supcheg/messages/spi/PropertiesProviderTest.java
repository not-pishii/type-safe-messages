package me.supcheg.messages.spi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesProviderTest {

    @Test
    void returnsParsedPropertiesKeyedByName() {
        ResourceOpener opener = fileName -> fileName.equals("messages_ru.properties")
                ? Optional.of((Reader) new StringReader("playerJoined=Игрок {player} зашёл"))
                : Optional.empty();
        PropertiesProvider provider = new PropertiesProvider("messages", opener);

        var result = provider.templates(Locale.forLanguageTag("ru"));

        assertThat(result.right()).hasValueSatisfying(content -> assertThat(content)
                .containsEntry("playerJoined", "Игрок {player} зашёл"));
    }

    @Test
    void reportsSourceProblemWhenFileMissing() {
        ResourceOpener opener = fileName -> Optional.empty();
        PropertiesProvider provider = new PropertiesProvider("messages", opener);

        var result = provider.templates(Locale.forLanguageTag("ru"));

        assertThat(result.left()).hasValueSatisfying(problems -> {
            assertThat(problems).hasSize(1);
            assertThat(problems.get(0).locale()).isEqualTo(Locale.forLanguageTag("ru"));
            assertThat(problems.get(0).description()).contains("messages_ru.properties");
        });
    }

    @Test
    void reportsSourceProblemOnIoFailure() throws IOException {
        ResourceOpener opener = fileName -> {
            throw new UncheckedIOException(new IOException("boom"));
        };
        PropertiesProvider provider = new PropertiesProvider("messages", opener);

        var result = provider.templates(Locale.forLanguageTag("ru"));

        assertThat(result.left())
                .hasValueSatisfying(
                        problems -> assertThat(problems.get(0).description()).contains("boom"));
    }
}
