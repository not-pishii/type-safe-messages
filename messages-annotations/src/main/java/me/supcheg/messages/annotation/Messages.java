package me.supcheg.messages.annotation;

import java.lang.annotation.*;

/** Помечает интерфейс-контракт сообщений: один type-параметр T, каждый метод возвращает T. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Messages {
}
