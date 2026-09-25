package me.supcheg.messages.processor;

import me.supcheg.javafile.JavaFile;
import me.supcheg.javafile.filer.JavaFileWriter;
import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.annotation.Messages;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.file.Path;
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
                    .map(ContractWriter::write)
                    .ifPresent(file -> writeSource(file, element));
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(MessageBundle.class)) {
            processBundle((TypeElement) element);
        }
        return true;
    }

    private void processBundle(TypeElement bundleElement) {
        BundleValidator.resolve(bundleElement, processingEnv).ifPresent(model -> {
            boolean usesDefaultProvider = BundleValidator.isDefaultProvider(model.providerElement(), processingEnv);
            String dirOption = processingEnv.getOptions().get(OPTION_MESSAGES_DIR);
            if (usesDefaultProvider && dirOption == null) {
                processingEnv
                        .getMessager()
                        .printMessage(
                                Diagnostic.Kind.ERROR,
                                "@MessageBundle requires the 'messages.dir' processor option"
                                        + " (pass -Amessages.dir=<path> or apply the messages bundle convention plugin)",
                                bundleElement);
                return;
            }
            Path messagesDir = dirOption != null ? Path.of(dirOption) : null;
            BundleValidator.validate(model, messagesDir, processingEnv).ifPresent(byLocale -> {
                var file =
                        switch (model.resolution()) {
                            case COMPILE_TIME -> CompileTimeBundleWriter.write(model, byLocale);
                            case RUNTIME -> RuntimeBundleWriter.write(model);
                        };
                writeSource(file, bundleElement);
            });
        });
    }

    private void writeSource(JavaFile file, Element origin) {
        try {
            JavaFileWriter.writeTo(file, processingEnv.getFiler(), origin);
        } catch (IOException e) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "failed to write " + file.qualifiedName() + ": " + e.getMessage(),
                            origin);
        }
    }
}
