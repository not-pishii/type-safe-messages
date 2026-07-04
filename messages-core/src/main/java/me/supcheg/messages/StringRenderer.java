package me.supcheg.messages;

import java.util.List;

public final class StringRenderer implements MessageRenderer<String> {

    private static final StringRenderer INSTANCE = new StringRenderer();

    public static StringRenderer instance() {
        return INSTANCE;
    }

    private StringRenderer() {}

    @Override
    public String literal(String text) {
        return text;
    }

    @Override
    public String argument(Object value) {
        return String.valueOf(value);
    }

    @Override
    public String concat(List<String> parts) {
        return String.join("", parts);
    }
}
