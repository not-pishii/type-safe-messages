package me.supcheg.messages.processor;

import me.supcheg.messages.Literal;
import me.supcheg.messages.MessageTemplate;
import me.supcheg.messages.Placeholder;

import java.util.Map;
import java.util.stream.Collectors;

final class CompileTimeBundleWriter {

    private CompileTimeBundleWriter() {}

    static String generatedName(BundleModel model) {
        return model.contract().simpleName() + "Bundle";
    }

    static String write(BundleModel model, Map<String, Map<String, MessageTemplate>> byLocale) {
        String contract = model.contract().simpleName();
        String contractFqn = model.contract().packageName() + "." + contract;
        String bundleName = generatedName(model);

        String localesSet = model.localeTags().stream()
                .map(tag -> "Locale.forLanguageTag(\"" + tag + "\")")
                .collect(Collectors.joining(", "));

        String factories = model.localeTags().stream()
                .map(tag -> """
                    public static <T> %s<T> %s(MessageRenderer<T> renderer) {
                        return new %s<>(renderer);
                    }
                """.formatted(contract, BundleNaming.methodName(tag), BundleNaming.className(tag)))
                .collect(Collectors.joining("\n"));

        String forLocaleCases = model.localeTags().stream()
                .map(tag -> "            case \"%s\" -> Optional.of(%s(renderer));"
                        .formatted(tag, BundleNaming.methodName(tag)))
                .collect(Collectors.joining("\n"));

        String impls = model.localeTags().stream()
                .map(tag -> localeImpl(model, tag, byLocale.get(tag)))
                .collect(Collectors.joining("\n"));

        return """
            package %s;

            import %s;

            import java.util.List;
            import java.util.Locale;
            import java.util.Optional;
            import java.util.Set;
            import java.util.function.Function;
            import me.supcheg.messages.Literal;
            import me.supcheg.messages.MessageRenderer;
            import me.supcheg.messages.MessageTemplate;
            import me.supcheg.messages.Placeholder;

            public final class %s {

                private static final Set<Locale> LOCALES = Set.of(%s);

                public static Set<Locale> locales() {
                    return LOCALES;
                }

            %s
                public static <T> Optional<%s<T>> forLocale(Locale locale, MessageRenderer<T> renderer) {
                    return switch (locale.toLanguageTag()) {
            %s
                        default -> Optional.empty();
                    };
                }

            %s
                private %s() {
                }
            }
            """.formatted(
                        model.packageName(),
                        contractFqn,
                        bundleName,
                        localesSet,
                        factories,
                        contract,
                        forLocaleCases,
                        impls,
                        bundleName);
    }

    private static String localeImpl(BundleModel model, String tag, Map<String, MessageTemplate> content) {
        String contract = model.contract().simpleName();
        String className = BundleNaming.className(tag);

        String templateFields = model.contract().messages().stream()
                .map(m -> "        private static final MessageTemplate %s = new MessageTemplate(\"%s\", List.of(%s));"
                        .formatted(
                                BundleNaming.constantName(m.methodName()),
                                JavaStrings.escape(m.key()),
                                templateParts(content.get(m.key()))))
                .collect(Collectors.joining("\n"));

        String methods = model.contract().messages().stream()
                .map(m -> """
                        @Override
                        public T %s(%s) {
                            Function<String, Object> args = %s;
                            return %s.render(renderer, args);
                        }
                """.formatted(
                                m.methodName(),
                                MethodSignatures.parameters(m),
                                MethodSignatures.argumentsFunction(m),
                                BundleNaming.constantName(m.methodName())))
                .collect(Collectors.joining("\n"));

        return """
                private record %s<T>(MessageRenderer<T> renderer) implements %s<T> {

            %s

            %s    }
            """.formatted(className, contract, templateFields, methods);
    }

    private static String templateParts(MessageTemplate template) {
        return template.parts().stream()
                .map(part -> switch (part) {
                    case Literal(String text) -> "new Literal(\"" + JavaStrings.escape(text) + "\")";
                    case Placeholder(String name) -> "new Placeholder(\"" + name + "\")";
                })
                .collect(Collectors.joining(", "));
    }
}
