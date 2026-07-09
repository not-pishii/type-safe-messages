package me.supcheg.messages.example.runtimedefault;

import me.supcheg.messages.spi.ResourceOpener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Opens {@code messages_*.properties} from the classpath instead of a real directory. */
final class ClasspathResourceOpener implements ResourceOpener {

    @Override
    public Optional<Reader> open(String fileName) throws IOException {
        InputStream in = ClasspathResourceOpener.class.getClassLoader().getResourceAsStream(fileName);
        return in == null ? Optional.empty() : Optional.of(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
}
