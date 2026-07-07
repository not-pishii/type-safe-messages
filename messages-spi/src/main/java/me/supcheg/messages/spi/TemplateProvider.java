package me.supcheg.messages.spi;

import me.supcheg.routine.Either;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Supplies raw, unparsed message templates (with {@code {placeholder}} syntax intact) for a
 * single locale. Implementations return an immutable snapshot keyed by message key; parsing the
 * templates and validating them against the contract's shape always happens in the library,
 * identically for compile-time and runtime resolution.
 *
 * <p>Implementations that ship translations as classpath resources inside their own jar should
 * anchor resource lookups on their own class, not the thread context class loader, and use a
 * package-scoped path to avoid collisions with other jars on the same classpath:
 *
 * <pre>{@code
 * public final class MyProvider implements TemplateProvider {
 *     public Either<List<SourceProblem>, Map<String, String>> templates(Locale locale) {
 *         String path = "com/example/translations/messages_" + locale.toLanguageTag().replace('-', '_') + ".json";
 *         try (InputStream in = MyProvider.class.getClassLoader().getResourceAsStream(path)) {
 *             if (in == null) {
 *                 return Either.left(List.of(new SourceProblem(locale, "classpath:/" + path + " not found")));
 *             }
 *             // ... read `in` as UTF-8 and parse into a Map<String, String>
 *         } catch (IOException e) {
 *             throw new UncheckedIOException(e);
 *         }
 *     }
 * }
 * }</pre>
 */
public interface TemplateProvider {

    Either<List<SourceProblem>, Map<String, String>> templates(Locale locale);
}
