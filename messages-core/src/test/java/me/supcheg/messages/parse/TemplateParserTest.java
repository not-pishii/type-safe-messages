package me.supcheg.messages.parse;

import me.supcheg.messages.Literal;
import me.supcheg.messages.Placeholder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateParserTest {

    @Test
    void parsesLiteralsAndPlaceholders() {
        var result = TemplateParser.parse("k", "Привет, {player}! Монет: {coins}");

        assertThat(result.right()).hasValueSatisfying(template -> assertThat(template.parts())
                .containsExactly(
                        new Literal("Привет, "),
                        new Placeholder("player"),
                        new Literal("! Монет: "),
                        new Placeholder("coins")));
    }

    @Test
    void escapedBraceIsLiteral() {
        var result = TemplateParser.parse("k", "json: \\{not a placeholder}");

        assertThat(result.right()).hasValueSatisfying(template -> assertThat(template.parts())
                .containsExactly(new Literal("json: {not a placeholder}")));
    }

    @Test
    void unclosedBraceIsInvalid() {
        var result = TemplateParser.parse("k", "oops {player");

        assertThat(result.left()).hasValueSatisfying(invalid -> {
            assertThat(invalid.position()).isEqualTo(5);
            assertThat(invalid.reason()).contains("unclosed");
        });
    }

    @Test
    void escapedBackslashIsLiteral() {
        var result = TemplateParser.parse("k", "a\\\\b");

        assertThat(result.right())
                .hasValueSatisfying(template -> assertThat(template.parts()).containsExactly(new Literal("a\\b")));
    }

    @Test
    void invalidPlaceholderNameIsInvalid() {
        var result = TemplateParser.parse("k", "{1bad}");

        assertThat(result.left()).hasValueSatisfying(invalid -> {
            assertThat(invalid.position()).isEqualTo(1);
            assertThat(invalid.reason()).contains("1bad");
        });
    }

    @Test
    void emptyTemplateIsValid() {
        assertThat(TemplateParser.parse("k", "").right()).isPresent();
    }
}
