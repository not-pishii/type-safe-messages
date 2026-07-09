package me.supcheg.messages.example.app;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void printsAllLocalesFromTheServiceLoadedBundle() {
        var out = new ByteArrayOutputStream();
        Main.printAll(new PrintStream(out, true, StandardCharsets.UTF_8));

        String printed = out.toString(StandardCharsets.UTF_8);
        assertThat(printed).contains("[ru] Игрок Steve зашёл на сервер");
        assertThat(printed).contains("[ru] У Steve на счету 10 монет");
        assertThat(printed).contains("[en] Player Steve joined the server");
        assertThat(printed).contains("[en] Steve has 10 coins");
    }
}
