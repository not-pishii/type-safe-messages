package me.supcheg.messages.example.runtimedefault;

import me.supcheg.messages.StringRenderer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeDefaultGameMessagesSourceTest {

    @Test
    void loadsBothLocalesFromClasspathProperties() {
        var byLocale = new RuntimeDefaultGameMessagesSource().load(StringRenderer.instance());

        assertThat(byLocale).containsOnlyKeys(Locale.forLanguageTag("ru"), Locale.forLanguageTag("en"));
        assertThat(byLocale.get(Locale.forLanguageTag("ru")).playerJoined("Steve"))
                .isEqualTo("Игрок Steve зашёл на сервер");
        assertThat(byLocale.get(Locale.forLanguageTag("ru")).balance("Steve", 10))
                .isEqualTo("У Steve на счету 10 монет");
        assertThat(byLocale.get(Locale.forLanguageTag("en")).balance("Steve", 10))
                .isEqualTo("Steve has 10 coins");
    }
}
