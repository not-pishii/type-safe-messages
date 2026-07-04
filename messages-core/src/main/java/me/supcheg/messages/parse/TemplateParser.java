package me.supcheg.messages.parse;

import me.supcheg.messages.Literal;
import me.supcheg.messages.MessageTemplate;
import me.supcheg.messages.Placeholder;
import me.supcheg.messages.TemplatePart;

import java.util.ArrayList;
import java.util.List;

/** Парсер шаблонов {@code {name}} с экранированием {@code \{} и {@code \\}. */
public final class TemplateParser {

    private TemplateParser() {}

    public static ParseResult parse(String key, String raw) {
        List<TemplatePart> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length() && (raw.charAt(i + 1) == '{' || raw.charAt(i + 1) == '\\')) {
                literal.append(raw.charAt(i + 1));
                i += 2;
            } else if (c == '{') {
                int close = raw.indexOf('}', i + 1);
                if (close < 0) {
                    return new ParseResult.Invalid(key, i, "unclosed '{'");
                }
                String name = raw.substring(i + 1, close);
                if (!isValidName(name)) {
                    return new ParseResult.Invalid(key, i + 1, "invalid placeholder name: '" + name + "'");
                }
                if (!literal.isEmpty()) {
                    parts.add(new Literal(literal.toString()));
                    literal.setLength(0);
                }
                parts.add(new Placeholder(name));
                i = close + 1;
            } else {
                literal.append(c);
                i++;
            }
        }
        if (!literal.isEmpty()) {
            parts.add(new Literal(literal.toString()));
        }
        return new ParseResult.Parsed(new MessageTemplate(key, List.copyOf(parts)));
    }

    private static boolean isValidName(String name) {
        if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        return name.chars().skip(1).allMatch(Character::isJavaIdentifierPart);
    }
}
