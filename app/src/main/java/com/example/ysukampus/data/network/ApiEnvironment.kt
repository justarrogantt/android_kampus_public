package com.example.ysukampus.data.network

import android.content.Context
import com.example.ysukampus.BuildConfig

/**
 * Окружение бэкенда, выбранное в отладочной сборке.
 *
 * Переключатель включён в debug, beta и betaRelease (последний уходит тестерам:
 * тест и прод — разные серверы с разными ботами, и без выбора сборка заперта в
 * том, что пришло в `BASE_URL`). В release он выключен через
 * `BuildConfig.SERVER_SWITCH_ENABLED`, и приложение всегда ходит на
 * `BuildConfig.BASE_URL`.
 *
 * Выбор виден, только если `BASE_URL_TEST` и `BASE_URL_PROD` действительно
 * различаются (см. [isSwitchAvailable]): когда их нет в local.properties, оба
 * падают обратно на `BASE_URL`, и переключать становится нечего.
 */
enum class ApiEnvironment(
    val title: String,
    /** Подпись для компактного чипа, которому не хватает места на полное название. */
    val shortTitle: String,
    val baseUrl: String,
    /** Бот, выдающий код подтверждения для этого бэкенда. */
    val telegramBotLink: String
) {
    Test("Тестовый", "Тест", BuildConfig.BASE_URL_TEST, BuildConfig.TELEGRAM_BOT_LINK_TEST),
    Prod("Прод", "Прод", BuildConfig.BASE_URL_PROD, BuildConfig.TELEGRAM_BOT_LINK_PROD);

    companion object {
        fun fromName(value: String?): ApiEnvironment? =
            entries.firstOrNull { it.name == value }

        /** Окружение, совпадающее с адресом сборки по умолчанию. */
        val default: ApiEnvironment
            get() = entries.firstOrNull { it.baseUrl == BuildConfig.BASE_URL } ?: Test
    }
}

/**
 * Держит текущий базовый URL в памяти процесса.
 *
 * Зачем отдельный holder, а не чтение SharedPreferences прямо в фабрике:
 * `createPublicAuthService()` вызывается без `Context`, а лезть за диском на
 * каждое создание Retrofit — лишняя работа. Поэтому значение читается один раз
 * при старте ([init]) и дальше отдаётся из памяти.
 *
 * `@Volatile` — потому что запись идёт из UI-потока, а чтение может происходить
 * из фонового при создании сервисов.
 */
object ApiEnvironmentHolder {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_ENVIRONMENT = "api_environment"

    @Volatile
    private var current: ApiEnvironment = ApiEnvironment.default

    /** Базовый URL для Retrofit. В неотладочных сборках всегда адрес сборки. */
    val baseUrl: String
        get() = if (BuildConfig.SERVER_SWITCH_ENABLED) current.baseUrl else BuildConfig.BASE_URL

    val environment: ApiEnvironment
        get() = current

    /**
     * Ссылка на Telegram-бота выбранного окружения. Код подтверждения выдаёт
     * бэкенд, а принимает его бот, поэтому при переключении сервера меняется и
     * бот. В неотладочных сборках — бот адреса сборки.
     */
    val telegramBotLink: String
        get() = if (BuildConfig.SERVER_SWITCH_ENABLED) {
            current.telegramBotLink.trim()
        } else {
            BuildConfig.TELEGRAM_BOT_LINK.trim()
        }

    /** Доступен ли выбор: переключатель включён и адреса действительно разные. */
    val isSwitchAvailable: Boolean
        get() = BuildConfig.SERVER_SWITCH_ENABLED &&
            ApiEnvironment.Test.baseUrl != ApiEnvironment.Prod.baseUrl

    /** Вызывается один раз при старте до создания любых сетевых сервисов. */
    fun init(context: Context) {
        if (!BuildConfig.SERVER_SWITCH_ENABLED) return

        val stored = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ENVIRONMENT, null)

        current = ApiEnvironment.fromName(stored) ?: ApiEnvironment.default
    }

    /**
     * Сохраняет выбор. Уже созданные Retrofit-клиенты продолжат смотреть на
     * старый адрес, поэтому вызывающий обязан перезапустить процесс —
     * см. `restartApp()` в UI переключателя.
     */
    fun select(context: Context, environment: ApiEnvironment) {
        if (!BuildConfig.SERVER_SWITCH_ENABLED) return

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENVIRONMENT, environment.name)
            // commit(): сразу после select() вызывающий убивает процесс
            // (restartApp -> exitProcess), и асинхронная запись не успела бы.
            .commit()

        current = environment
    }
}
