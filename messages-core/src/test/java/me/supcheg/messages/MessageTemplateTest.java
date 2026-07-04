package me.supcheg.messages;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTemplateTest {

    @Test
    void rendersLiteralsAndPlaceholdersInOrder() {
        var template = new MessageTemplate(
                "balance",
                List.of(
                        new Literal("У "),
                        new Placeholder("player"),
                        new Literal(" на счету "),
                        new Placeholder("coins"),
                        new Literal(" монет")));
        Map<String, Object> args = Map.of("player", "Steve", "coins", 10);

        String result = template.render(StringRenderer.instance(), args::get);

        assertThat(result).isEqualTo("У Steve на счету 10 монет");
    }

    @Test
    void partsListIsDefensivelyCopied() {
        var mutable = new java.util.ArrayList<TemplatePart>(List.of(new Literal("a")));
        var template = new MessageTemplate("k", mutable);
        mutable.add(new Literal("b"));

        assertThat(template.parts()).containsExactly(new Literal("a"));
    }

    @Test
    void nullArgumentRendersAsStringNull() {
        var template = new MessageTemplate("k", List.of(new Placeholder("x")));

        assertThat(template.render(StringRenderer.instance(), name -> null)).isEqualTo("null");
    }
}
