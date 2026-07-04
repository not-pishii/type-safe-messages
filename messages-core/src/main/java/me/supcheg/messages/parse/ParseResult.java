package me.supcheg.messages.parse;

import me.supcheg.messages.MessageTemplate;

public sealed interface ParseResult {

    record Parsed(MessageTemplate template) implements ParseResult {
    }

    record Invalid(String key, int position, String reason) implements ParseResult {
    }
}
