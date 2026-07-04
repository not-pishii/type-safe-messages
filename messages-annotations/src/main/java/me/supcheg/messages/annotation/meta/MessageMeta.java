package me.supcheg.messages.annotation.meta;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface MessageMeta {
    String key();
    String method();
    ParamMeta[] params() default {};
}
