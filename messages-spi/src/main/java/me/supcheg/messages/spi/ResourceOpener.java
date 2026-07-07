package me.supcheg.messages.spi;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

/** Opens a named resource for reading, or reports it as absent. */
public interface ResourceOpener {

    Optional<Reader> open(String fileName) throws IOException;
}
