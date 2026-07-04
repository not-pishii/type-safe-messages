package me.supcheg.messages.processor;

import me.supcheg.messages.annotation.Key;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.ERROR;

final class ContractValidator {

    private ContractValidator() {}

    static Optional<ContractModel> validate(TypeElement iface, ProcessingEnvironment env) {
        var messager = env.getMessager();
        boolean valid = true;

        if (iface.getKind() != ElementKind.INTERFACE) {
            messager.printMessage(ERROR, "@Messages supports only interfaces", iface);
            return Optional.empty();
        }
        if (iface.getTypeParameters().size() != 1) {
            messager.printMessage(
                    ERROR,
                    "@Messages interface must declare exactly one type parameter (the render result type)",
                    iface);
            return Optional.empty();
        }
        TypeParameterElement typeParam = iface.getTypeParameters().getFirst();
        String typeParamName = typeParam.getSimpleName().toString();

        List<ContractModel.MessageModel> messages = new ArrayList<>();
        Set<String> methodNames = new HashSet<>();
        Set<String> keys = new HashSet<>();

        for (ExecutableElement method : ElementFilter.methodsIn(iface.getEnclosedElements())) {
            if (method.isDefault() || method.getModifiers().contains(Modifier.STATIC)) {
                messager.printMessage(ERROR, "@Messages methods must be abstract (no default/static)", method);
                valid = false;
                continue;
            }
            if (!method.getTypeParameters().isEmpty()) {
                messager.printMessage(ERROR, "@Messages methods must not declare type parameters", method);
                valid = false;
            }
            if (!(method.getReturnType() instanceof TypeVariable tv)
                    || !tv.asElement().equals(typeParam)) {
                messager.printMessage(
                        ERROR,
                        "@Messages method must return the contract type parameter '" + typeParamName + "'",
                        method);
                valid = false;
            }
            String name = method.getSimpleName().toString();
            if (!methodNames.add(name)) {
                messager.printMessage(ERROR, "@Messages does not allow method overloads: '" + name + "'", method);
                valid = false;
            }
            Key keyAnnotation = method.getAnnotation(Key.class);
            String key = keyAnnotation != null ? keyAnnotation.value() : name;
            if (!keys.add(key)) {
                messager.printMessage(ERROR, "duplicate message key '" + key + "'", method);
                valid = false;
            }
            List<ContractModel.ParamModel> params = method.getParameters().stream()
                    .map(p -> new ContractModel.ParamModel(p.getSimpleName().toString(), p.asType()))
                    .toList();
            messages.add(new ContractModel.MessageModel(key, name, params));
        }

        if (!valid) {
            return Optional.empty();
        }
        String packageName =
                env.getElementUtils().getPackageOf(iface).getQualifiedName().toString();
        return Optional.of(
                new ContractModel(packageName, iface.getSimpleName().toString(), typeParamName, List.copyOf(messages)));
    }
}
