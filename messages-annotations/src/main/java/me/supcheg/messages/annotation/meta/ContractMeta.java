package me.supcheg.messages.annotation.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Внутренняя аннотация: машиночитаемое описание контракта на сгенерированном классе. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ContractMeta {
    MessageMeta[] value();
}
