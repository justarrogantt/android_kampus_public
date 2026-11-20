package com.example.ysukampus.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Crashlytics для типов сборки, которые уходят тестерам, — `beta` и
 * `betaRelease`. Заглушка для остальных — в `app/src/crashOff/`, подключение
 * обоих наборов — в `app/build.gradle.kts` (блок `sourceSets`).
 *
 * Инициализировать SDK руками не нужно: Firebase поднимается своим
 * `ContentProvider` по данным из `google-services.json`, а Crashlytics сам
 * перехватывает необработанные исключения. Отдельного вызова в `MainActivity`
 * нет намеренно — его легко потерять при правках точки входа.
 *
 * Собираются только краши. Firebase Analytics не подключён, и подключать его не
 * надо: без него Crashlytics теряет ровно одну вещь — список последних событий
 * перед падением, то есть тот самый сбор поведения, которого здесь быть не
 * должно. Личные данные в отчёт не попадают: тела запросов Crashlytics не
 * видит, а свои ключи мы не проставляем.
 */
object CrashReporting {

    /** Отчёты о крашах уходят в Crashlytics. */
    const val IS_ENABLED: Boolean = true

    /**
     * Осознанно роняет приложение, чтобы один раз убедиться, что отчёты
     * доходят и стектрейс деобфусцирован.
     *
     * Отчёт уходит не в момент падения, а при следующем запуске приложения:
     * Crashlytics успевает только записать его на диск.
     */
    fun triggerTestCrash() {
        FirebaseCrashlytics.getInstance().apply {
            log("Тестовый краш, вызванный вручную из профиля")
            setCustomKey("test_crash", true)
        }
        throw TestCrashException()
    }
}

/**
 * Отдельный тип, а не голый `RuntimeException`: в списке Crashlytics по имени
 * сразу видно, что это проверка канала, а не настоящее падение.
 */
class TestCrashException : RuntimeException(
    "Ysukampus test crash — вызван вручную из профиля"
)
