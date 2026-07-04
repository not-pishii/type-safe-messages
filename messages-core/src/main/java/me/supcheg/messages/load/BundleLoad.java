package me.supcheg.messages.load;

import java.util.List;
import java.util.function.Function;

/** Either-подобный результат загрузки контента. */
public sealed interface BundleLoad<M> {

    record Loaded<M>(M messages) implements BundleLoad<M> {
    }

    record Failed<M>(List<ContentProblem> problems) implements BundleLoad<M> {
        public Failed {
            problems = List.copyOf(problems);
        }
    }

    default <R> BundleLoad<R> map(Function<? super M, ? extends R> f) {
        return switch (this) {
            case Loaded<M>(M m) -> new Loaded<>(f.apply(m));
            case Failed<M>(List<ContentProblem> p) -> new Failed<>(p);
        };
    }
}
