package me.supcheg.messages.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class ContractValidationTest {

    private static Compilation compile(String source) {
        return javac().withProcessors(new MessagesProcessor())
                .compile(JavaFileObjects.forSourceString("com.example.GameMessages", source));
    }

    @Test
    void validContractCompiles() {
        var compilation = compile("""
            package com.example;

            import me.supcheg.messages.annotation.Messages;

            @Messages
            public interface GameMessages<T> {
                T playerJoined(String player);
            }
            """);

        assertThat(compilation).succeededWithoutWarnings();
    }

    @Test
    void methodReturningWrongTypeFails() {
        var compilation = compile("""
            package com.example;

            import me.supcheg.messages.annotation.Messages;

            @Messages
            public interface GameMessages<T> {
                String playerJoined(String player);
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must return the contract type parameter");
    }

    @Test
    void overloadsFail() {
        var compilation = compile("""
            package com.example;

            import me.supcheg.messages.annotation.Messages;

            @Messages
            public interface GameMessages<T> {
                T msg(String a);
                T msg(String a, String b);
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("overload");
    }

    @Test
    void defaultAndStaticMethodsFail() {
        var compilation = compile("""
            package com.example;

            import me.supcheg.messages.annotation.Messages;

            @Messages
            public interface GameMessages<T> {
                default T greeting(String player) {
                    return null;
                }

                static String helper() {
                    return "";
                }
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("no default/static");
    }

    @Test
    void genericMethodFails() {
        var compilation = compile("""
            package com.example;

            import me.supcheg.messages.annotation.Messages;

            @Messages
            public interface GameMessages<T> {
                <X> T generic(X value);
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must not declare type parameters");
    }

    @Test
    void duplicateKeysFail() {
        var compilation = compile("""
            package com.example;

            import me.supcheg.messages.annotation.Key;
            import me.supcheg.messages.annotation.Messages;

            @Messages
            public interface GameMessages<T> {
                T playerJoined(String player);

                @Key("playerJoined")
                T anotherJoined(String player);
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("duplicate message key 'playerJoined'");
    }

    @Test
    void twoTypeParametersFail() {
        var compilation = compile("""
            package com.example;

            import me.supcheg.messages.annotation.Messages;

            @Messages
            public interface GameMessages<T, U> {
                T playerJoined(String player);
            }
            """);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("exactly one type parameter");
    }

    @Test
    void classAnnotatedWithMessagesFails() {
        var compilation = javac().withProcessors(new MessagesProcessor())
                .compile(JavaFileObjects.forSourceString("com.example.NotAnInterface", """
                package com.example;

                import me.supcheg.messages.annotation.Messages;

                @Messages
                public class NotAnInterface {
                }
                """));

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("only interfaces");
    }
}
