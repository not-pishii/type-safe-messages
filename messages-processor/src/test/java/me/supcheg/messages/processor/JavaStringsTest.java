package me.supcheg.messages.processor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaStringsTest {

    @Test
    void escapesSpecialCharacters() {
        assertThat(JavaStrings.escape("a\"b\\c\nd\te\rf")).isEqualTo("a\\\"b\\\\c\\nd\\te\\rf");
    }

    @Test
    void keepsUnicodeAsIs() {
        assertThat(JavaStrings.escape("Игрок {x}")).isEqualTo("Игрок {x}");
    }
}
