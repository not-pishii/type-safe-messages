package me.supcheg.messages.load;

import me.supcheg.messages.StringRenderer;
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
        new MessageShape("balance", "balance", List.of("player", "coins"))
    ));

    @TempDir
    Path dir;

    @Test
    void loadsValidBundle() throws IOException {
        Files.writeString(dir.resolve("messages_ru.properties"), """
            playerJoined=Игрок {player} зашёл
            balance=У {player} на счету {coins}
            """);

        var result = BundleLoader.load(dir, Locale.of("ru"), "messages", SHAPE);

        assertThat(result).isInstanceOfSatisfying(BundleLoad.Loaded.class, loaded -> {
            @SuppressWarnings("unchecked")
            var content = (Map<String, me.supcheg.messages.MessageTemplate>) loaded.messages();
            assertThat(content.get("balance").render(StringRenderer.instance(),
                Map.<String, Object>of("player", "Steve", "coins", 10)::get))
                .isEqualTo("У Steve на счету 10");
        });
    }

    @Test
    void missingFileFails() {
        var result = BundleLoader.load(dir, Locale.of("de"), "messages", SHAPE);

        assertThat(result).isInstanceOfSatisfying(BundleLoad.Failed.class, failed ->
            assertThat(failed.problems()).singleElement().isInstanceOf(ContentProblem.MissingFile.class));
    }

    @Test
    void missingKeyAndUnknownPlaceholderAreBothReported() throws IOException {
        Files.writeString(dir.resolve("messages_ru.properties"), """
            playerJoined=Игрок {playr} зашёл
            """);

        var result = BundleLoader.load(dir, Locale.of("ru"), "messages", SHAPE);

        assertThat(result).isInstanceOfSatisfying(BundleLoad.Failed.class, failed ->
            assertThat(failed.problems())
                .hasExactlyElementsOfTypes(ContentProblem.UnknownPlaceholder.class, ContentProblem.MissingKey.class));
    }

    @Test
    void malformedTemplateIsReportedWithPositionAndReason() throws IOException {
        Files.writeString(dir.resolve("messages_ru.properties"), """
            playerJoined=Игрок {player зашёл
            balance=У {player} на счету {coins}
            """);

        var result = BundleLoader.load(dir, Locale.of("ru"), "messages", SHAPE);

        assertThat(result).isInstanceOfSatisfying(BundleLoad.Failed.class, failed -> {
            @SuppressWarnings("unchecked")
            var problems = (List<ContentProblem>) failed.problems();
            assertThat(problems).singleElement()
                .isInstanceOfSatisfying(ContentProblem.MalformedTemplate.class, problem -> {
                    assertThat(problem.key()).isEqualTo("playerJoined");
                    assertThat(problem.reason()).contains("unclosed");
                });
        });
    }

    @Test
    void mapTransformsLoadedAndKeepsFailed() {
        BundleLoad<Integer> loaded = new BundleLoad.Loaded<>(21);
        BundleLoad<Integer> failed = new BundleLoad.Failed<>(List.of());

        assertThat(loaded.map(x -> x * 2)).isEqualTo(new BundleLoad.Loaded<>(42));
        assertThat(failed.map(x -> x * 2)).isInstanceOf(BundleLoad.Failed.class);
    }
}
