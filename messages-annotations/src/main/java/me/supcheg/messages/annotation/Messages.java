package me.supcheg.messages.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Помечает интерфейс-контракт сообщений: один type-параметр T, каждый метод возвращает T. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Messages {}
