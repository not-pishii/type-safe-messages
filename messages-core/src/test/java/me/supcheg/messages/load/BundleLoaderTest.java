package me.supcheg.messages.load;

import me.supcheg.messages.StringRenderer;
import me.supcheg.messages.spi.PathResourceOpener;
import me.supcheg.messages.spi.PropertiesProvider;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BundleLoaderTest {

    private static final ContractShape SHAPE = new ContractShape(List.of(
            new MessageShape("playerJoined", "playerJoined", List.of("player")),
            new MessageShape("balance", "balance", List.of("player", "coins"))));

    @TempDir
    Path dir;

    @Test
    void loadsValidBundle() throws IOException {
        Files.writeString(dir.resolve("messages_ru.properties"), """
            playerJoined=Игрок {player} зашёл
            balance=У {player} на счету {coins}
            """);

        var result = BundleLoader.load(dir, Locale.of("ru"), "messages", SHAPE);

        assertThat(result.right()).hasValueSatisfying(content -> assertThat(content.get("balance")
                        .render(StringRenderer.instance(), Map.<String, Object>of("player", "Steve", "coins", 10)::get))
                .isEqualTo("У Steve на счету 10"));
    }

    @Test
    void missingFileFails() {
        var result = BundleLoader.load(dir, Locale.of("de"), "messages", SHAPE);

        assertThat(result.left())
                .hasValueSatisfying(problems ->
                        assertThat(problems).singleElement().isInstanceOf(ContentProblem.SourceProblem.class));
    }

    @Test
    void missingKeyAndUnknownPlaceholderAreBothReported() throws IOException {
        Files.writeString(dir.resolve("messages_ru.properties"), """
            playerJoined=Игрок {playr} зашёл
            """);

        var result = BundleLoader.load(dir, Locale.of("ru"), "messages", SHAPE);

        assertThat(result.left()).hasValueSatisfying(problems -> assertThat(problems)
                .hasExactlyElementsOfTypes(ContentProblem.UnknownPlaceholder.class, ContentProblem.MissingKey.class));
    }

    @Test
    void malformedTemplateIsReportedWithPositionAndReason() throws IOException {
        Files.writeString(dir.resolve("messages_ru.properties"), """
            playerJoined=Игрок {player зашёл
            balance=У {player} на счету {coins}
            """);

        var result = BundleLoader.load(dir, Locale.of("ru"), "messages", SHAPE);

        assertThat(result.left()).hasValueSatisfying(problems -> assertThat(problems)
                .singleElement()
                .isInstanceOfSatisfying(ContentProblem.MalformedTemplate.class, problem -> {
                    assertThat(problem.key()).isEqualTo("playerJoined");
                    assertThat(problem.reason()).contains("unclosed");
                }));
    }

    @Test
    void pathOverloadAndProviderOverloadAgreeOnValidInput() throws IOException {
        Files.writeString(dir.resolve("messages_ru.properties"), """
            playerJoined=Игрок {player} зашёл
            balance=У {player} на счету {coins}
            """);

        Either<List<ContentProblem>, Map<String, me.supcheg.messages.MessageTemplate>> viaPath =
                BundleLoader.load(dir, Locale.of("ru"), "messages", SHAPE);

        TemplateProvider provider = new PropertiesProvider("messages", new PathResourceOpener(dir));
        Either<List<ContentProblem>, Map<String, me.supcheg.messages.MessageTemplate>> viaProvider =
                BundleLoader.load(provider, Locale.of("ru"), SHAPE);

        assertThat(viaProvider.right().isPresent()).isEqualTo(viaPath.right().isPresent());
        assertThat(viaProvider.right()).isEqualTo(viaPath.right());
    }
}
