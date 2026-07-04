package me.supcheg.messages.processor;

import me.supcheg.messages.MessageTemplate;
import me.supcheg.messages.Placeholder;
import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.annotation.meta.ContractMeta;
import me.supcheg.messages.annotation.meta.MessageMeta;
import me.supcheg.messages.annotation.meta.ParamMeta;
import me.supcheg.messages.parse.ParseResult;
import me.supcheg.messages.parse.TemplateParser;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;

final class BundleValidator {

    private BundleValidator() {}

    static Optional<BundleModel> resolve(TypeElement bundleElement, ProcessingEnvironment env) {
        MessageBundle annotation = bundleElement.getAnnotation(MessageBundle.class);
        TypeMirror contractMirror = contractType(annotation);
        TypeElement contractInterface = (TypeElement) env.getTypeUtils().asElement(contractMirror);
        if (contractInterface == null) {
            env.getMessager().printMessage(ERROR, "cannot resolve contract type", bundleElement);
            return Optional.empty();
        }

        Optional<ContractModel> contract =
                fromMeta(contractInterface, env).or(() -> ContractValidator.validate(contractInterface, env));
        if (contract.isEmpty()) {
            env.getMessager()
                    .printMessage(
                            ERROR,
                            "contract metadata not found for " + contractInterface.getQualifiedName()
                                    + "; make sure the contract module is compiled with messages-processor",
                            bundleElement);
            return Optional.empty();
        }

        String packageName = env.getElementUtils()
                .getPackageOf(bundleElement)
                .getQualifiedName()
                .toString();
        return Optional.of(new BundleModel(
                contractInterface,
                contract.get(),
                List.of(annotation.locales()),
                annotation.resolution(),
                annotation.resources(),
                packageName));
    }

    /** Восстанавливает ContractModel из сгенерированного <Simple>Contract на classpath (имена параметров!). */
    private static Optional<ContractModel> fromMeta(TypeElement contractInterface, ProcessingEnvironment env) {
        String metaFqn = contractInterface.getQualifiedName() + "Contract";
        TypeElement metaElement = env.getElementUtils().getTypeElement(metaFqn);
        if (metaElement == null) {
            return Optional.empty();
        }
        ContractMeta meta = metaElement.getAnnotation(ContractMeta.class);
        if (meta == null) {
            return Optional.empty();
        }
        // типы параметров берём из interface-элемента (байткод), имена — из меты; связываем по имени метода
        Map<String, ExecutableElement> methods =
                ElementFilter.methodsIn(contractInterface.getEnclosedElements()).stream()
                        .collect(Collectors.toMap(m -> m.getSimpleName().toString(), m -> m));

        List<ContractModel.MessageModel> messages = new ArrayList<>();
        for (MessageMeta messageMeta : meta.value()) {
            ExecutableElement method = methods.get(messageMeta.method());
            if (method == null) {
                return Optional.empty(); // мета рассинхронизирована с интерфейсом — форсируем перекомпиляцию контракта
            }
            List<ContractModel.ParamModel> params = new ArrayList<>();
            ParamMeta[] paramMetas = messageMeta.params();
            for (int i = 0; i < paramMetas.length; i++) {
                params.add(new ContractModel.ParamModel(
                        paramMetas[i].name(), method.getParameters().get(i).asType()));
            }
            messages.add(new ContractModel.MessageModel(messageMeta.key(), messageMeta.method(), List.copyOf(params)));
        }
        String packageName = env.getElementUtils()
                .getPackageOf(contractInterface)
                .getQualifiedName()
                .toString();
        String typeParamName =
                contractInterface.getTypeParameters().getFirst().getSimpleName().toString();
        return Optional.of(new ContractModel(
                packageName, contractInterface.getSimpleName().toString(), typeParamName, List.copyOf(messages)));
    }

    static Optional<Map<String, Map<String, MessageTemplate>>> validate(
            BundleModel model, Path messagesDir, ProcessingEnvironment env) {
        var messager = env.getMessager();
        boolean valid = true;
        Map<String, Map<String, MessageTemplate>> byLocale = new LinkedHashMap<>();

        for (String tag : model.localeTags()) {
            Path file = messagesDir.resolve(model.resources() + "_" + tag.replace('-', '_') + ".properties");
            if (!Files.isRegularFile(file)) {
                messager.printMessage(ERROR, "[" + tag + "] translations file not found: " + file);
                valid = false;
                continue;
            }
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                messager.printMessage(ERROR, "[" + tag + "] cannot read " + file + ": " + e.getMessage());
                valid = false;
                continue;
            }

            Map<String, MessageTemplate> content = new HashMap<>();
            for (ContractModel.MessageModel message : model.contract().messages()) {
                String raw = properties.getProperty(message.key());
                if (raw == null) {
                    messager.printMessage(ERROR, "[" + tag + "] missing message key '" + message.key() + "'");
                    valid = false;
                    continue;
                }
                Set<String> paramNames = message.params().stream()
                        .map(ContractModel.ParamModel::name)
                        .collect(Collectors.toSet());
                switch (TemplateParser.parse(message.key(), raw)) {
                    case ParseResult.Invalid(String key, int position, String reason) -> {
                        messager.printMessage(
                                ERROR,
                                "[" + tag + "] key '" + key + "': malformed template at " + position + ": " + reason);
                        valid = false;
                    }
                    case ParseResult.Parsed(MessageTemplate template) -> {
                        Set<String> used = template.parts().stream()
                                .filter(p -> p instanceof Placeholder)
                                .map(p -> ((Placeholder) p).name())
                                .collect(Collectors.toSet());
                        for (String name : used) {
                            if (!paramNames.contains(name)) {
                                messager.printMessage(
                                        ERROR,
                                        "[" + tag + "] key '" + message.key() + "': unknown placeholder '{" + name
                                                + "}', expected one of " + paramNames);
                                valid = false;
                            }
                        }
                        for (String name : paramNames) {
                            if (!used.contains(name)) {
                                messager.printMessage(
                                        WARNING,
                                        "[" + tag + "] key '" + message.key() + "': parameter '" + name
                                                + "' is not used in the template");
                            }
                        }
                        content.put(message.key(), template);
                    }
                }
            }
            Set<String> knownKeys = model.contract().messages().stream()
                    .map(ContractModel.MessageModel::key)
                    .collect(Collectors.toSet());
            for (String key : properties.stringPropertyNames()) {
                if (!knownKeys.contains(key)) {
                    messager.printMessage(WARNING, "[" + tag + "] key '" + key + "' is not declared in the contract");
                }
            }
            byLocale.put(tag, Map.copyOf(content));
        }
        return valid ? Optional.of(byLocale) : Optional.empty();
    }

    private static TypeMirror contractType(MessageBundle annotation) {
        try {
            annotation.contract();
            throw new IllegalStateException("MirroredTypeException expected");
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }
}
