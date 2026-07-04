package me.supcheg.messages.processor;

import javax.lang.model.type.TypeMirror;
import java.util.stream.Collectors;

final class ContractWriter {

    private ContractWriter() {
    }

    static String generatedName(ContractModel model) {
        return model.simpleName() + "Contract";
    }

    static String write(ContractModel model) {
        String metaEntries = model.messages().stream()
            .map(m -> {
                String params = m.params().stream()
                    .map(p -> "@ParamMeta(name = \"%s\", type = \"%s\")".formatted(p.name(), typeToString(p.type())))
                    .collect(Collectors.joining(", "));
                return "    @MessageMeta(key = \"%s\", method = \"%s\", params = {%s})"
                    .formatted(JavaStrings.escape(m.key()), m.methodName(), params);
            })
            .collect(Collectors.joining(",\n"));

        String shapeEntries = model.messages().stream()
            .map(m -> {
                String placeholders = m.params().stream()
                    .map(p -> "\"" + p.name() + "\"")
                    .collect(Collectors.joining(", "));
                return "        new MessageShape(\"%s\", \"%s\", List.of(%s))"
                    .formatted(JavaStrings.escape(m.key()), m.methodName(), placeholders);
            })
            .collect(Collectors.joining(",\n"));

        return """
            package %s;

            import java.util.List;
            import me.supcheg.messages.annotation.meta.ContractMeta;
            import me.supcheg.messages.annotation.meta.MessageMeta;
            import me.supcheg.messages.annotation.meta.ParamMeta;
            import me.supcheg.messages.load.ContractShape;
            import me.supcheg.messages.load.MessageShape;

            @ContractMeta({
            %s
            })
            public final class %s {

                public static final ContractShape SHAPE = new ContractShape(List.of(
            %s
                ));

                private %s() {
                }
            }
            """.formatted(model.packageName(), metaEntries, generatedName(model), shapeEntries, generatedName(model));
    }

    private static String typeToString(TypeMirror type) {
        return type.toString();
    }
}
