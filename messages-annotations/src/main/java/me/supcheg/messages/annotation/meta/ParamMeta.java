package me.supcheg.messages.annotation.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ParamMeta {
    String name();

    String type();
}
