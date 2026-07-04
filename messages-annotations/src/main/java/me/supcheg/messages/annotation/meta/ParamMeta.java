package me.supcheg.messages.annotation.meta;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ParamMeta {
    String name();
    String type();
}
