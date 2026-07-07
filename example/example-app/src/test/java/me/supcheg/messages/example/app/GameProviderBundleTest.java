package me.supcheg.messages.example.app;

import me.supcheg.messages.StringRenderer;
import me.supcheg.messages.example.provider.GameMessagesBundle;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class GameProviderBundleTest {

    @Test
    void russianMessagesRenderExactly() {
        var ru = GameMessagesBundle.ru(StringRenderer.instance());

        assertThat(ru.playerJoined("Steve")).isEqualTo("Steve подключился к игре");
        assertThat(ru.balance("Steve", 10)).isEqualTo("Счёт игрока Steve: 10 монет");
    }

    @Test
    void englishMessagesRenderExactly() {
        var en = GameMessagesBundle.en(StringRenderer.instance());

        assertThat(en.playerJoined("Steve")).isEqualTo("Steve connected to the game");
        assertThat(en.balance("Steve", 10)).isEqualTo("Account of Steve: 10 coins");
    }

    @Test
    void unknownLocaleIsEmpty() {
        assertThat(GameMessagesBundle.forLocale(Locale.GERMAN, StringRenderer.instance()))
                .isEmpty();
    }
}
