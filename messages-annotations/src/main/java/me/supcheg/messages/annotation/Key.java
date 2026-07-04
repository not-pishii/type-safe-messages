package me.supcheg.messages.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Переопределяет ключ сообщения (по умолчанию — имя метода). */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Key {
    String value();
}
