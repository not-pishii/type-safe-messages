package me.supcheg.messages.annotation.meta;

import java.lang.annotation.*;

/** Внутренняя аннотация: машиночитаемое описание контракта на сгенерированном классе. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ContractMeta {
    MessageMeta[] value();
}
