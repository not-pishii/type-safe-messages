package me.supcheg.messages.example.app;

import me.supcheg.messages.StringRenderer;
import me.supcheg.messages.example.GameMessagesBundle;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class GameMessagesTest {

    @Test
    void russianMessagesRenderExactly() {
        var ru = GameMessagesBundle.ru(StringRenderer.instance());

        assertThat(ru.playerJoined("Steve")).isEqualTo("Игрок Steve зашёл на сервер");
        assertThat(ru.balance("Steve", 10)).isEqualTo("У Steve на счету 10 монет");
    }

    @Test
    void englishMessagesRenderExactly() {
        var en = GameMessagesBundle.en(StringRenderer.instance());

        assertThat(en.balance("Steve", 10)).isEqualTo("Steve has 10 coins");
    }

    @Test
    void unknownLocaleIsEmpty() {
        assertThat(GameMessagesBundle.forLocale(Locale.GERMAN, StringRenderer.instance()))
                .isEmpty();
    }
}
