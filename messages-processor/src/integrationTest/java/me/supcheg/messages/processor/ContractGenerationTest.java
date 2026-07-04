package me.supcheg.messages.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class ContractGenerationTest {

    @Test
    void generatesContractMetaClass() {
        Compilation compilation = javac()
            .withProcessors(new MessagesProcessor())
            .compile(JavaFileObjects.forSourceString("com.example.GameMessages", """
                package com.example;

                import me.supcheg.messages.annotation.Key;
                import me.supcheg.messages.annotation.Messages;

                @Messages
                public interface GameMessages<T> {
                    T playerJoined(String player);

                    @Key("player.balance")
                    T balance(String player, int coins);
                }
                """));

        assertThat(compilation).succeeded();
        var contents = assertThat(compilation).generatedSourceFile("com.example.GameMessagesContract")
            .contentsAsUtf8String();
        contents.contains("@ContractMeta(");
        contents.contains("key = \"player.balance\"");
        contents.contains("method = \"balance\"");
        contents.contains("@ParamMeta(name = \"coins\", type = \"int\")");
        contents.contains("public static final ContractShape SHAPE");
        contents.contains("new MessageShape(\"playerJoined\", \"playerJoined\", List.of(\"player\"))");
    }
}
