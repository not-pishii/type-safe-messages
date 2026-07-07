package me.supcheg.messages.processor;

import java.util.stream.Collectors;

final class RuntimeBundleWriter {

    private RuntimeBundleWriter() {}

    static String generatedName(BundleModel model) {
        return model.contract().simpleName() + "RuntimeBundle";
    }

    static String write(BundleModel model) {
        String contract = model.contract().simpleName();
        String contractFqn = model.contract().packageName() + "." + contract;
        String contractMetaName = contractFqn + "Contract";
        String bundleName = generatedName(model);

        String methods = model.contract().messages().stream()
                .map(m -> """
                        @Override
                        public T %s(%s) {
                            Function<String, Object> args = %s;
                            return content.get("%s").render(renderer, args);
                        }
                """.formatted(
                                m.methodName(),
                                MethodSignatures.parameters(m),
                                MethodSignatures.argumentsFunction(m),
                                JavaStrings.escape(m.key())))
                .collect(Collectors.joining("\n"));

        return """
            package %s;

            import %s;

            import java.nio.file.Path;
            import java.util.Locale;
            import java.util.Map;
            import java.util.List;
            import java.util.function.Function;
            import me.supcheg.routine.Either;
            import me.supcheg.messages.MessageRenderer;
            import me.supcheg.messages.MessageTemplate;
            import me.supcheg.messages.load.BundleLoader;
            import me.supcheg.messages.load.ContentProblem;
            import me.supcheg.messages.spi.TemplateProvider;

            public final class %s {

                public static <T> Either<List<ContentProblem>, %s<T>> load(Path dir, Locale locale, MessageRenderer<T> renderer) {
                    return BundleLoader.load(dir, locale, "%s", %s.SHAPE)
                        .mapRight(content -> new Impl<>(content, renderer));
                }

                public static <T> Either<List<ContentProblem>, %s<T>> load(TemplateProvider provider, Locale locale, MessageRenderer<T> renderer) {
                    return BundleLoader.load(provider, locale, %s.SHAPE)
                        .mapRight(content -> new Impl<>(content, renderer));
                }

                private record Impl<T>(Map<String, MessageTemplate> content, MessageRenderer<T> renderer)
                        implements %s<T> {

            %s    }

                private %s() {
                }
            }
            """.formatted(
                        model.packageName(),
                        contractFqn,
                        bundleName,
                        contract,
                        JavaStrings.escape(model.resources()),
                        contractMetaName,
                        contract,
                        contractMetaName,
                        contract,
                        methods,
                        bundleName);
    }
}
