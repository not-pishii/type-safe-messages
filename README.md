# type-safe-messages

[![Maven Central](https://img.shields.io/maven-central/v/me.supcheg/messages-processor)](https://central.sonatype.com/artifact/me.supcheg/messages-processor)
[![Javadoc](https://javadoc.io/badge2/me.supcheg/messages-processor/javadoc.svg)](https://javadoc.io/doc/me.supcheg/messages-processor)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://github.com/not-pishii/type-safe-messages/actions/workflows/build.yml/badge.svg)](https://github.com/not-pishii/type-safe-messages/actions/workflows/build.yml)

Compile-time safe localized message formatting for Java.

## The problem / The solution

Localized messages built with `MessageFormat` (or plain string concatenation) are
just strings. Nothing stops you from swapping arguments, misspelling a
placeholder, or forgetting a translation for one of your locales — the compiler
has no idea these strings are meant to have a shape.

**Before**, with `MessageFormat`:

```java
// "{0}" and "{1}" are just indices — the compiler can't catch a swap.
String template = "У {0} на счету {1} монет";
String message = MessageFormat.format(template, coins, player); // oops, swapped!
```

Nothing here fails until a human reads the rendered text in the wrong locale.

**After**, with type-safe-messages: you declare a contract interface, and the
annotation processor generates a bundle that can only be called with the right
arguments, in the right order, for every declared locale.

```java
@Messages
public interface GameMessages<T> {
    T playerJoined(String player);
    T balance(String player, int coins);
}
```

```java
GameMessages<String> ru = GameMessagesBundle.ru(StringRenderer.instance());
ru.balance("Steve", 10); // "У Steve на счету 10 монет" — arguments can't be swapped, it won't compile otherwise
```

If a translation is missing a placeholder, uses an unknown one, or a locale
file is missing entirely, the build fails (compile-time resolution) or loading
reports precise problems (runtime resolution) — never a silently wrong string
in production.

## Quick start

### 1. Dependencies

```kotlin
dependencies {
    // the contract module: annotation + generated-code runtime support
    compileOnly("me.supcheg:messages-annotations:1.2.0")
    implementation("me.supcheg:messages-core:1.2.0")
    annotationProcessor("me.supcheg:messages-processor:1.2.0")
}
```

(`compileOnly`/`api` for `messages-annotations` depending on whether the
annotation needs to be visible to your module's consumers.)

### 2. Declare the contract

```java
package me.supcheg.messages.example;

import me.supcheg.messages.annotation.Messages;

@Messages
public interface GameMessages<T> {

    T playerJoined(String player);

    T balance(String player, int coins);
}
```

### 3. Declare a bundle backed by properties files

```java
package me.supcheg.messages.example;

import me.supcheg.messages.annotation.MessageBundle;

@MessageBundle(contract = GameMessages.class, locales = {"ru", "en"})
final class GameBundle {
}
```

with `src/main/messages/messages_ru.properties`:

```properties
playerJoined=Игрок {player} зашёл на сервер
balance=У {player} на счету {coins} монет
```

and `src/main/messages/messages_en.properties`:

```properties
playerJoined=Player {player} joined the server
balance={player} has {coins} coins
```

Since the properties files are annotation-processor input rather than regular
resources, the module applying `messages-processor` needs to point the
processor at that directory. The convention plugin used by this repo's own
example modules looks like this:

```kotlin
// messages.bundle-conventions.gradle.kts
plugins {
    java
}

val messagesDir = layout.projectDirectory.dir("src/main/messages")

tasks.named<JavaCompile>("compileJava") {
    inputs.dir(messagesDir)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("messagesDir")
    options.compilerArgs.add("-Amessages.dir=${messagesDir.asFile.absolutePath}")
}
```

### 4. Use the generated bundle

The processor generates `GameMessagesBundle` next to your contract, with one
accessor per declared locale plus a lookup by `Locale`:

```java
package me.supcheg.messages.example.app;

import me.supcheg.messages.StringRenderer;
import me.supcheg.messages.example.GameMessages;
import me.supcheg.messages.example.GameMessagesBundle;

public final class Main {

    public static void main(String[] args) {
        GameMessages<String> ru = GameMessagesBundle.ru(StringRenderer.instance());
        System.out.println(ru.playerJoined("Steve"));
        System.out.println(ru.balance("Steve", 10));
    }

    private Main() {
    }
}
```

`StringRenderer.instance()` is the built-in `MessageRenderer<String>` that
concatenates parts into a plain `String`. You can implement your own
`MessageRenderer<T>` (e.g. to render into a chat component type, a `Component`,
or styled text) by implementing three methods:

```java
public interface MessageRenderer<T> {
    T literal(String text);
    T argument(Object value);
    T concat(List<T> parts);
}
```

`GameMessagesBundle` also exposes locale lookup and the set of supported
locales:

```java
Set<Locale> supported = GameMessagesBundle.locales();
Optional<GameMessages<String>> maybe =
        GameMessagesBundle.forLocale(Locale.GERMAN, StringRenderer.instance()); // Optional.empty()
```

## Runtime resolution

For cases where translations should be editable/loadable without recompiling
(e.g. shipped alongside a server jar and reloaded), declare the bundle with
`resolution = Resolution.RUNTIME`:

```java
package me.supcheg.messages.example;

import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.annotation.Resolution;

@MessageBundle(contract = GameMessages.class, locales = {"ru", "en"}, resolution = Resolution.RUNTIME)
final class GameRuntimeBundleDecl {
}
```

The processor still generates a fully typed bundle — `GameMessagesRuntimeBundle`
— but instead of baking the translations in, it loads and validates them from a
directory of `.properties` files at runtime, returning a `Either`
(`Error`/`Result`) so callers must handle both outcomes:

```java
GameMessagesRuntimeBundle.load(Path.of(args[0]), Locale.of("ru"), StringRenderer.instance())
        .fold(
        problems -> problems.stream().map(p -> "PROBLEM: " + p.describe()),
loaded -> Stream.of(loaded.playerJoined("Steve")))
        .forEach(IO::println);
```

Each `ContentProblem` pinpoints what went wrong (`SourceProblem`,
`MissingKey`, `MalformedTemplate`, `UnknownPlaceholder`) with the offending
locale, key, and reason — no more guessing why a translation didn't load.

Because each snapshot is immutable, a live-reload pattern is just: call `load`
again and swap the reference behind an `AtomicReference`, e.g.:

```java
AtomicReference<GameMessages<String>> current = new AtomicReference<>(initial);
// later, e.g. on a file-watch event or admin command:
GameMessagesRuntimeBundle.load(dir, locale, StringRenderer.instance())
        .fold(
        problems -> { problems.forEach(p -> System.out.println("PROBLEM: " + p.describe())); return null; },
        loaded -> { current.set(loaded); return null; });
```

The same `load(TemplateProvider, Locale, MessageRenderer)` overload works with any
`TemplateProvider`, not just the default `Path`-backed one — pass an instance
with its own state (e.g. a connection pool) instead of a directory.

## Custom translation sources

Both compile-time and runtime resolution can be backed by a translation source
other than `.properties` files on disk — a JSON file bundled as a classpath
resource, a database, a translation-management service — by implementing
`me.supcheg.messages.spi.TemplateProvider` and pointing the bundle at it:

```java
package me.supcheg.messages.example;

import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.translations.JsonTemplateProvider;

@MessageBundle(contract = GameMessages.class, locales = {"ru", "en"}, provider = JsonTemplateProvider.class)
final class GameJsonBundle {
}
```

`JsonTemplateProvider` here lives in its own module (e.g. `translations`) so it
can be depended on separately from the module declaring the bundle. The
consuming module needs the provider on the annotation processor's classpath to
resolve it at compile time, and on its own compile classpath to reference the
generated code that mentions it:

```kotlin
dependencies {
    annotationProcessor(project(":translations"))
    compileOnly(project(":translations"))
}
```

For `Resolution.RUNTIME` bundles, add the provider as a regular runtime
dependency too, since the generated `TemplateProvider`-based `load` overload
references it at runtime, not just at compile time:

```kotlin
dependencies {
    annotationProcessor(project(":translations"))
    compileOnly(project(":translations"))
    implementation(project(":translations"))
}
```

A provider used at `COMPILE_TIME` must be a deterministic function of its own
artifact — no database calls, no network, no wall-clock reads; for a live
source (a database, a translation-management service), use `RUNTIME`
resolution, or materialize the data into a resource file with a separate
Gradle task feeding the provider's own module.

See `example/example-translations` and `example/example-bundle-provider` in
this repository for a complete, compiling version of this pattern.

## How it works

The annotation processor, given `@Messages`-annotated contracts and
`@MessageBundle` declarations, runs in four stages:

1. **Validate the contract** — every abstract method on the `@Messages`
   interface must return the interface's own type parameter `T` and take only
   named, supported argument types; violations fail the build with a clear
   diagnostic.
2. **Derive metadata** — for each method, the processor records its message
   key and the ordered set of expected placeholder names, producing a
   `ContractMeta`/`MessageMeta` shape shared by both compile-time and runtime
   generation.
3. **Validate translations against that metadata** — every declared locale's
   `.properties` file is parsed and checked: every method must have a key,
   every placeholder in the template must be one of the expected argument
   names, and every template must parse. Compile-time bundles fail the build
   on any mismatch; runtime bundles report the same checks as `ContentProblem`s
   at load time.
4. **Generate the bundle class** — a `<Contract>Bundle` (compile-time) or
   `<Contract>RuntimeBundle` (runtime) implementing the contract for each
   locale, wired to `MessageRenderer<T>` so the same generated code renders to
   any target type.

## Modules & requirements

Requires **Java 25+**.

| Module | Purpose |
|---|---|
| `messages-annotations` | `@Messages`, `@MessageBundle`, `@Key`, `Resolution` — the annotations you apply to your own code. |
| `messages-core` | Runtime support used by generated code: `MessageRenderer`, `StringRenderer`, `MessageTemplate`, and the runtime-loading types (`BundleLoader`, `ContentProblem`). |
| `messages-processor` | The annotation processor that validates contracts/translations and generates bundle classes. |
| `messages-spi` | The `TemplateProvider` SPI and the default `PropertiesProvider` implementation. Depend on this directly only if you're writing a custom provider. |

The `example/` modules in this repository (`example-contract`,
`example-bundle-main`, `example-bundle-alt`, `example-app`) are a compiling,
tested, end-to-end demonstration of the whole flow and are the source of every
snippet above.

## License

MIT
