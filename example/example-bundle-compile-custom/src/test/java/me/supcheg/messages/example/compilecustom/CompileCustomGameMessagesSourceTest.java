package me.supcheg.messages.example.compilecustom;

import me.supcheg.messages.StringRenderer;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CompileCustomGameMessagesSourceTest {

    @Test
    void loadsBothLocalesFromTheJsonProvider() {
        var byLocale = new CompileCustomGameMessagesSource().load(StringRenderer.instance());

        assertThat(byLocale).containsOnlyKeys(Locale.forLanguageTag("ru"), Locale.forLanguageTag("en"));
        assertThat(byLocale.get(Locale.forLanguageTag("ru")).playerJoined("Steve"))
                .isEqualTo("Steve подключился к игре");
        assertThat(byLocale.get(Locale.forLanguageTag("ru")).balance("Steve", 10))
                .isEqualTo("Счёт игрока Steve: 10 монет");
        assertThat(byLocale.get(Locale.forLanguageTag("en")).playerJoined("Steve"))
                .isEqualTo("Steve connected to the game");
        assertThat(byLocale.get(Locale.forLanguageTag("en")).balance("Steve", 10))
                .isEqualTo("Account of Steve: 10 coins");
    }
}
