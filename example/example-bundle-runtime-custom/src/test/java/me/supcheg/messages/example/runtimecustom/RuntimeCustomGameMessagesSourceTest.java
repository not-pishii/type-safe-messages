package me.supcheg.messages.example.runtimecustom;

import me.supcheg.messages.StringRenderer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeCustomGameMessagesSourceTest {

    @Test
    void loadsBothLocalesFromTheJsonProviderAtRuntime() {
        var byLocale = new RuntimeCustomGameMessagesSource().load(StringRenderer.instance());

        assertThat(byLocale).containsOnlyKeys(Locale.forLanguageTag("ru"), Locale.forLanguageTag("en"));
        assertThat(byLocale.get(Locale.forLanguageTag("ru")).playerJoined("Steve"))
                .isEqualTo("Steve подключился к игре");
        assertThat(byLocale.get(Locale.forLanguageTag("en")).balance("Steve", 10))
                .isEqualTo("Account of Steve: 10 coins");
    }
}
