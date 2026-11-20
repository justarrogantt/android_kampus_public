package com.example.ysukampus.data.network.di

import com.example.ysukampus.BuildConfig
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Один OkHttpClient на процесс.
 *
 * Раньше каждая фабрика ViewModel собирала себе новый клиент, а значит и новый
 * connection pool с новым пулом потоков: соединения между вкладками не
 * переиспользовались, и каждое открытие экрана начиналось с нового TCP+TLS
 * handshake. Теперь варианты уровней авторизации собираются поверх [newBuilder],
 * и общим у них становится ровно одно — [ConnectionPool], ради которого всё и
 * затевалось.
 *
 * А вот [Dispatcher] у каждого клиента обязан быть свой, и это не деталь вкуса.
 * `newBuilder()` копирует ссылку на исходный dispatcher, а у него есть лимит
 * `maxRequestsPerHost`. Общий dispatcher приводил к дедлоку: главная шлёт пачку
 * запросов на один хост, все они получают 401 и синхронно виснут в
 * `TokenAuthentificator.authenticate()`, занимая слоты, а `POST auth/refresh`
 * встаёт в очередь за слотом, который освободится только после этого самого
 * refresh. Запросы не возвращались, экраны висели на скелетонах.
 *
 * Потоки при этом не плодятся: все dispatcher'ы делят один [ExecutorService].
 *
 * Таймауты заданы явно: дефолтные 10 секунд OkHttp короче, чем бэкенду иногда
 * требуется на выдачу расписания, и такой таймаут прилетал в UI как
 * «проверьте соединение с интернетом» при живом интернете.
 */
internal object HttpClientProvider {

    /** Установка соединения — если за это время не поднялось, сеть действительно плохая. */
    private const val CONNECT_TIMEOUT_SECONDS = 15L

    /** Ожидание ответа. Бэк ходит за расписанием во внешний источник, ему нужен запас. */
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    /**
     * Потолок на весь вызов, включая редиректы и повтор запроса после обновления
     * токена в TokenAuthentificator. Должен быть заметно больше read-таймаута,
     * иначе он обрежет законный retry с новым access-токеном.
     */
    private const val CALL_TIMEOUT_SECONDS = 60L

    /** Дефолт OkHttp — 5, и ровно столько запросов главная шлёт на старте. */
    private const val MAX_REQUESTS_PER_HOST = 16

    /**
     * Общий пул соединений — то, ради чего клиенты вообще сводились вместе:
     * TCP+TLS-соединение, поднятое расписанием, переиспользует форум.
     */
    private val sharedConnectionPool = ConnectionPool()

    /**
     * Один пул потоков на все dispatcher'ы. Конфигурация совпадает с той, что
     * OkHttp создаёт для dispatcher'а по умолчанию (кэширующий пул без очереди).
     */
    private val sharedDispatcherExecutor: ExecutorService by lazy {
        Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "YsuOkHttp Dispatcher").apply { isDaemon = false }
        }
    }

    private val base: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(sharedConnectionPool)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply { loggingInterceptor()?.let(::addNetworkInterceptor) }
            .build()
    }

    /**
     * Билдер с общими таймаутами и общим пулом соединений — но с собственным
     * [Dispatcher], изолирующим лимиты одного клиента от остальных.
     */
    fun newBuilder(): OkHttpClient.Builder = base.newBuilder()
        .dispatcher(
            Dispatcher(sharedDispatcherExecutor).apply {
                // Главная поднимает расписание, погоду, форум, профиль и
                // уведомления разом — дефолтных пяти на хост ей мало.
                maxRequestsPerHost = MAX_REQUESTS_PER_HOST
            }
        )

    /**
     * Лог запросов в logcat (тег `OkHttp`) для debug и beta. В release и
     * betaRelease `HTTP_LOG_ENABLED` — константа `false`, ветка сворачивается
     * R8, и сама библиотека в APK не попадает.
     *
     * Именно network-, а не application-интерсептор: так в логе видны заголовки,
     * которые добавили DeviceIdInterceptor и AuthInterceptor, реальные
     * редиректы и повтор запроса после обновления токена в
     * TokenAuthentificator — то есть ровно то, что нужно, чтобы отличить
     * таймаут от 503 и от 401.
     *
     * `Authorization` и `X-Device-Id` вырезаны: logcat читает любое приложение
     * с доступом к adb, и access-токену там не место.
     */
    private fun loggingInterceptor(): HttpLoggingInterceptor? {
        if (!BuildConfig.HTTP_LOG_ENABLED) return null

        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
            redactHeader("X-Device-Id")
        }
    }
}
