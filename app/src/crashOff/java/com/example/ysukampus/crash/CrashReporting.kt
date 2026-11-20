package com.example.ysukampus.crash

/**
 * Заглушка для типов сборки без Crashlytics — `debug` и `release`.
 * Рабочая реализация лежит в `app/src/crashOn/`, подключение обоих наборов —
 * в `app/build.gradle.kts` (блок `sourceSets`).
 *
 * `IS_ENABLED` — `const`, а не `val`: в `release` R8 сворачивает по нему ветку
 * с пунктом тестового краша и выбрасывает её вместе с текстами.
 */
object CrashReporting {

    /** Отчёты о крашах в этом варианте не собираются. */
    const val IS_ENABLED: Boolean = false

    /** Не вызывается: UI спрятан под `IS_ENABLED`. */
    fun triggerTestCrash() = Unit
}
