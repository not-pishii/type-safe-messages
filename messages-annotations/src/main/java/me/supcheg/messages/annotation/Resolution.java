package me.supcheg.messages.annotation;

public enum Resolution {
    /** Переводы зашиваются в сгенерированный класс на компиляции. */
    COMPILE_TIME,
    /** Переводы загружаются из внешнего каталога с валидацией при загрузке. */
    RUNTIME,
}
