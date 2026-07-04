package me.supcheg.messages.processor;

import java.util.stream.Collectors;

final class MethodSignatures {

    private MethodSignatures() {}

    static String parameters(ContractModel.MessageModel message) {
        return message.params().stream().map(p -> p.type() + " " + p.name()).collect(Collectors.joining(", "));
    }

    /** Тело Function<String,Object> для подстановки аргументов. */
    static String argumentsFunction(ContractModel.MessageModel message) {
        if (message.params().isEmpty()) {
            return "name -> null";
        }
        String cases = message.params().stream()
                .map(p -> "                case \"%s\" -> %s;".formatted(p.name(), p.name()))
                .collect(Collectors.joining("\n"));
        return """
            name -> switch (name) {
            %s
                            default -> throw new IllegalStateException(name);
                        }""".formatted(cases);
    }
}
