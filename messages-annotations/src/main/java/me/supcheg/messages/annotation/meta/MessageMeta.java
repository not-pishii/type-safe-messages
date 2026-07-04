package me.supcheg.messages.annotation.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface MessageMeta {
    String key();

    String method();

    ParamMeta[] params() default {};
}
