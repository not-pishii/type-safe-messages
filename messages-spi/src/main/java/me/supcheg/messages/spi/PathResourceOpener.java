package me.supcheg.messages.spi;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Opens resources as files inside a directory (UTF-8). Used for both the runtime {@code Path}
 * directory and the annotation processor's {@code messages.dir} option.
 */
public final class PathResourceOpener implements ResourceOpener {

    private final Path dir;

    public PathResourceOpener(Path dir) {
        this.dir = dir;
    }

    @Override
    public Optional<Reader> open(String fileName) throws IOException {
        var file = dir.resolve(fileName);
        return Files.isRegularFile(file)
                ? Optional.of(Files.newBufferedReader(file, StandardCharsets.UTF_8))
                : Optional.empty();
    }
}
