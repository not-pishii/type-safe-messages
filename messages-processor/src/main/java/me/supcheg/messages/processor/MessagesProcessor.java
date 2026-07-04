package me.supcheg.messages.processor;

import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.annotation.Messages;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

public final class MessagesProcessor extends AbstractProcessor {

    public static final String OPTION_MESSAGES_DIR = "messages.dir";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Messages.class.getCanonicalName(), MessageBundle.class.getCanonicalName());
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(OPTION_MESSAGES_DIR);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Messages.class)) {
            ContractValidator.validate((TypeElement) element, processingEnv)
                .ifPresent(model -> writeSource(
                    model.packageName() + "." + ContractWriter.generatedName(model),
                    ContractWriter.write(model),
                    element));
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(MessageBundle.class)) {
            processBundle((TypeElement) element);
        }
        return true;
    }

    private void processBundle(TypeElement bundleElement) {
        String dirOption = processingEnv.getOptions().get(OPTION_MESSAGES_DIR);
        if (dirOption == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "@MessageBundle requires the 'messages.dir' processor option"
                    + " (pass -Amessages.dir=<path> or apply the messages bundle convention plugin)",
                bundleElement);
            return;
        }
        BundleValidator.resolve(bundleElement, processingEnv).ifPresent(model ->
            BundleValidator.validate(model, java.nio.file.Path.of(dirOption), processingEnv).ifPresent(byLocale -> {
                String source = switch (model.resolution()) {
                    case COMPILE_TIME -> CompileTimeBundleWriter.write(model, byLocale);
                    case RUNTIME -> RuntimeBundleWriter.write(model);
                };
                String simpleName = switch (model.resolution()) {
                    case COMPILE_TIME -> CompileTimeBundleWriter.generatedName(model);
                    case RUNTIME -> RuntimeBundleWriter.generatedName(model);
                };
                writeSource(model.packageName() + "." + simpleName, source, bundleElement);
            }));
    }

    private void writeSource(String fqn, String source, Element origin) {
        try (var writer = processingEnv.getFiler().createSourceFile(fqn, origin).openWriter()) {
            writer.write(source);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR, "failed to write " + fqn + ": " + e.getMessage(), origin);
        }
    }
}
