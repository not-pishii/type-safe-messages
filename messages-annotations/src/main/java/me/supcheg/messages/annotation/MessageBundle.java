package me.supcheg.messages.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Декларация бандла переводов для контракта. Файлы: {@code <resources>_<tag>.properties}. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface MessageBundle {
    Class<?> contract();

    String[] locales();

    Resolution resolution() default Resolution.COMPILE_TIME;

    String resources() default "messages";
}
