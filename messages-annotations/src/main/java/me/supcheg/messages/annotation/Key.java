package me.supcheg.messages.annotation;

import java.lang.annotation.*;

/** Переопределяет ключ сообщения (по умолчанию — имя метода). */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Key {
    String value();
}
