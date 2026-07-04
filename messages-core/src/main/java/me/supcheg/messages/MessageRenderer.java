package me.supcheg.messages;

import java.util.List;

/** Собирает результат форматирования типа T из литералов и значений аргументов. */
public interface MessageRenderer<T> {
    T literal(String text);

    T argument(Object value);

    T concat(List<T> parts);
}
