package me.supcheg.messages.processor;

import java.util.Arrays;
import java.util.stream.Collectors;

final class BundleNaming {

    private BundleNaming() {
    }

    static String className(String tag) {
        return Arrays.stream(tag.split("-"))
            .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase())
            .collect(Collectors.joining());
    }

    static String methodName(String tag) {
        String className = className(tag);
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    static String constantName(String methodName) {
        return methodName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
    }
}
