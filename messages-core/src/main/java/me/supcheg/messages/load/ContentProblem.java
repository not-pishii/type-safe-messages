package me.supcheg.messages.load;

import java.util.Locale;
import java.util.Set;

public sealed interface ContentProblem {

    Locale locale();

    String describe();

    record SourceProblem(Locale locale, String description) implements ContentProblem {
        @Override
        public String describe() {
            return "[" + locale.toLanguageTag() + "] " + description;
        }
    }

    record MissingKey(Locale locale, String key) implements ContentProblem {
        @Override
        public String describe() {
            return "[" + locale.toLanguageTag() + "] missing message key '" + key + "'";
        }
    }

    record MalformedTemplate(Locale locale, String key, int position, String reason) implements ContentProblem {
        @Override
        public String describe() {
            return "[" + locale.toLanguageTag() + "] key '" + key + "': malformed template at " + position + ": "
                    + reason;
        }
    }

    record UnknownPlaceholder(Locale locale, String key, String placeholder, Set<String> expected)
            implements ContentProblem {
        public UnknownPlaceholder {
            expected = Set.copyOf(expected);
        }

        @Override
        public String describe() {
            return "[" + locale.toLanguageTag() + "] key '" + key + "': unknown placeholder '{" + placeholder
                    + "}', expected one of " + expected;
        }
    }
}
