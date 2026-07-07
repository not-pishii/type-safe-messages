package me.supcheg.messages.annotation;

import me.supcheg.messages.spi.PropertiesProvider;
import me.supcheg.messages.spi.TemplateProvider;

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

    Class<? extends TemplateProvider> provider() default PropertiesProvider.class;
}
