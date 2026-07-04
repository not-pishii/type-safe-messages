package me.supcheg.messages.parse;

import me.supcheg.messages.Literal;
import me.supcheg.messages.StringRenderer;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateParserProperties {

    @Property
    void textWithoutBracesParsesToSingleLiteral(@ForAll @StringLength(min = 1, max = 200) String text) {
        Assume.that(!text.contains("{") && !text.contains("\\") && !text.contains("}"));

        var result = TemplateParser.parse("k", text);

        assertThat(result).isInstanceOfSatisfying(ParseResult.Parsed.class, parsed -> assertThat(
                        parsed.template().parts())
                .containsExactly(new Literal(text)));
    }

    @Property
    void renderingParsedTemplateRestoresTextWithSubstitutions(
            @ForAll @AlphaChars @NotBlank @StringLength(min = 1, max = 10) String name,
            @ForAll @StringLength(max = 50) String value) {
        Assume.that(!value.contains("{") && !value.contains("\\") && !value.contains("}"));

        var result = TemplateParser.parse("k", "pre {" + name + "} post");

        assertThat(result).isInstanceOfSatisfying(ParseResult.Parsed.class, parsed -> assertThat(
                        parsed.template().render(StringRenderer.instance(), n -> value))
                .isEqualTo("pre " + value + " post"));
    }
}
