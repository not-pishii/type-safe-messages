package me.supcheg.messages.load;

import java.util.List;

public record MessageShape(String key, String method, List<String> placeholders) {

    public MessageShape {
        placeholders = List.copyOf(placeholders);
    }
}
