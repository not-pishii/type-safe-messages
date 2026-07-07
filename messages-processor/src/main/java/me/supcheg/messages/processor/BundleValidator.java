package me.supcheg.messages.processor;

import me.supcheg.messages.MessageTemplate;
import me.supcheg.messages.Placeholder;
import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.annotation.meta.ContractMeta;
import me.supcheg.messages.annotation.meta.MessageMeta;
import me.supcheg.messages.annotation.meta.ParamMeta;
import me.supcheg.messages.parse.ParseResult;
import me.supcheg.messages.parse.TemplateParser;
import me.supcheg.messages.spi.PathResourceOpener;
import me.supcheg.messages.spi.PropertiesProvider;
import me.supcheg.messages.spi.SourceProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
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

        TypeMirror providerMirror = providerType(annotation);
        TypeElement providerElement = (TypeElement) env.getTypeUtils().asElement(providerMirror);
        if (providerElement == null) {
            env.getMessager().printMessage(ERROR, "cannot resolve provider type", bundleElement);
            return Optional.empty();
        }

        if (!isDefaultProvider(providerElement, env)) {
            if (!annotation.resources().equals("messages")) {
                env.getMessager()
                        .printMessage(
                                ERROR,
                                "@MessageBundle.resources() has no effect together with a custom provider() ("
                                        + providerElement.getQualifiedName() + "); remove one of them",
                                bundleElement);
                return Optional.empty();
            }
            if (providerElement.getModifiers().contains(Modifier.ABSTRACT)) {
                env.getMessager()
                        .printMessage(
                                ERROR,
                                "provider " + providerElement.getQualifiedName() + " must not be abstract",
                                bundleElement);
                return Optional.empty();
            }
            boolean hasPublicNoArgConstructor =
                    ElementFilter.constructorsIn(providerElement.getEnclosedElements()).stream()
                            .anyMatch(c -> c.getParameters().isEmpty()
                                    && c.getModifiers().contains(Modifier.PUBLIC));
            if (!hasPublicNoArgConstructor) {
                env.getMessager()
                        .printMessage(
                                ERROR,
                                "provider " + providerElement.getQualifiedName() + " needs a public no-arg constructor",
                                bundleElement);
                return Optional.empty();
            }
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
                packageName,
                providerElement));
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

        boolean customProvider = !isDefaultProvider(model.providerElement(), env);
        TemplateProvider provider;
        if (customProvider) {
            Optional<TemplateProvider> loaded = loadCustomProvider(model.providerElement(), env);
            if (loaded.isEmpty()) {
                return Optional.empty();
            }
            provider = loaded.get();
        } else {
            provider = new PropertiesProvider(model.resources(), new PathResourceOpener(messagesDir));
        }

        for (String tag : model.localeTags()) {
            Either<List<SourceProblem>, Map<String, String>> snapshot;
            if (customProvider) {
                try {
                    snapshot = provider.templates(Locale.forLanguageTag(tag));
                } catch (Throwable e) {
                    messager.printMessage(
                            ERROR,
                            "[" + tag + "] provider " + model.providerElement().getQualifiedName()
                                    + " threw while producing templates: " + e.getMessage());
                    messager.printMessage(NOTE, stackTraceOf(e));
                    valid = false;
                    continue;
                }
            } else {
                snapshot = provider.templates(Locale.forLanguageTag(tag));
            }
            Optional<List<SourceProblem>> sourceProblems = snapshot.left();
            if (sourceProblems.isPresent()) {
                sourceProblems.get().forEach(p -> messager.printMessage(ERROR, "[" + tag + "] " + p.description()));
                valid = false;
                continue;
            }
            Map<String, String> properties = snapshot.right().orElseThrow();

            Map<String, MessageTemplate> content = new HashMap<>();
            for (ContractModel.MessageModel message : model.contract().messages()) {
                String raw = properties.get(message.key());
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
            for (String key : properties.keySet()) {
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

    private static TypeMirror providerType(MessageBundle annotation) {
        try {
            annotation.provider();
            throw new IllegalStateException("MirroredTypeException expected");
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }

    static boolean isDefaultProvider(TypeElement providerElement, ProcessingEnvironment env) {
        TypeElement defaultElement = env.getElementUtils().getTypeElement("me.supcheg.messages.spi.PropertiesProvider");
        return defaultElement != null
                && env.getTypeUtils().isSameType(providerElement.asType(), defaultElement.asType());
    }

    /**
     * Loads and instantiates a user-provided {@link TemplateProvider} via reflection, isolating
     * every failure mode (missing class, throwing constructor, reflective access issues) so that
     * one broken provider produces a diagnostic instead of crashing the whole {@code process()} round.
     */
    private static Optional<TemplateProvider> loadCustomProvider(
            TypeElement providerElement, ProcessingEnvironment env) {
        String fqn = providerElement.getQualifiedName().toString();
        try {
            Class<?> clazz = Class.forName(fqn, true, MessagesProcessor.class.getClassLoader());
            Object instance = clazz.getDeclaredConstructor().newInstance();
            return Optional.of((TemplateProvider) instance);
        } catch (ClassNotFoundException e) {
            env.getMessager()
                    .printMessage(
                            ERROR,
                            "provider class " + fqn + " not found on the annotation processor's classpath; "
                                    + "add its module to the 'annotationProcessor' configuration");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            env.getMessager()
                    .printMessage(ERROR, "provider " + fqn + " threw during construction: " + cause.getMessage());
            env.getMessager().printMessage(NOTE, stackTraceOf(cause));
        } catch (ReflectiveOperationException e) {
            env.getMessager().printMessage(ERROR, "cannot instantiate provider " + fqn + ": " + e.getMessage());
            env.getMessager().printMessage(NOTE, stackTraceOf(e));
        } catch (Throwable e) {
            env.getMessager().printMessage(ERROR, "cannot instantiate provider " + fqn + ": " + e.getMessage());
            env.getMessager().printMessage(NOTE, stackTraceOf(e));
        }
        return Optional.empty();
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
