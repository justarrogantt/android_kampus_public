package com.example.ysukampus.ui.admin.state

/** Право на просмотр и скачивание ежедневных отчётов. */
const val PERMISSION_VIEW_DAILY_REPORT = "view-daily-report"

/** Право поставить генерацию отчёта в очередь. */
const val PERMISSION_GENERATE_DAILY_REPORT = "generate-daily-report"

/** Подвкладки «Статистики» — те же три вида, что и в вебке. */
enum class AdminStatsView(val title: String) {
    Today("Сегодня"),
    Reports("Отчёты"),
    Chart("График")
}

/**
 * Девять метрик дневного отчёта. `key` — имя поля в файле отчёта и значение
 * параметра `metric` у `admin/reports/daily/series`.
 */
enum class AdminReportMetric(val key: String, val label: String) {
    TotalUsers("total_users", "Всего пользователей"),
    NewUsers("new_users", "Новых за день"),
    UsersActive("users_active", "Активных за день"),
    ScheduleViews("schedule_views", "Просмотры расписания"),
    ScheduleApiErrors("schedule_api_errors", "Ошибки API расписания"),
    PostsCreated("posts_created", "Новые посты"),
    CommentsCreated("comments_created", "Новые комментарии"),
    LikesCreated("likes_created", "Лайки"),
    ForumActiveUsers("forum_active_users", "Активных на форуме");

    companion object {
        fun fromKey(key: String): AdminReportMetric? = entries.firstOrNull { it.key == key }
    }
}

/** Группировка метрик для быстрого чтения — порядок и состав как в вебке. */
data class AdminReportMetricGroup(val title: String, val metrics: List<AdminReportMetric>)

val ADMIN_REPORT_METRIC_GROUPS = listOf(
    AdminReportMetricGroup(
        title = "Пользователи",
        metrics = listOf(
            AdminReportMetric.TotalUsers,
            AdminReportMetric.UsersActive,
            AdminReportMetric.NewUsers
        )
    ),
    AdminReportMetricGroup(
        title = "Расписание",
        metrics = listOf(
            AdminReportMetric.ScheduleViews,
            AdminReportMetric.ScheduleApiErrors
        )
    ),
    AdminReportMetricGroup(
        title = "Форум",
        metrics = listOf(
            AdminReportMetric.ForumActiveUsers,
            AdminReportMetric.PostsCreated,
            AdminReportMetric.CommentsCreated,
            AdminReportMetric.LikesCreated
        )
    )
)

/** Статус последней задачи генерации за дату. */
enum class AdminReportJobStatus(val label: String) {
    Queued("В очереди"),
    Running("Считается"),
    Done("Готов"),
    Failed("Ошибка");

    companion object {
        fun fromApi(raw: String?): AdminReportJobStatus? = when (raw?.lowercase()) {
            "queued" -> Queued
            "running" -> Running
            "done" -> Done
            "failed" -> Failed
            else -> null
        }
    }
}

/**
 * Строка списка отчётов. `status = null` — файл на диске есть, а задачи в БД
 * уже нет; `hasFile = false` — наоборот, задача осталась, а файл потерян.
 */
data class AdminReportListItem(
    /** Дата отчёта в формате `YYYY-MM-DD` — она же путь в API. */
    val date: String,
    val status: AdminReportJobStatus?,
    val hasFile: Boolean,
    val sizeBytes: Long?
) {
    val isActive: Boolean
        get() = status == AdminReportJobStatus.Queued || status == AdminReportJobStatus.Running
}

/** Содержимое отчёта за дату: девять метрик и время генерации. */
data class AdminDailyReport(
    val date: String,
    /** Приходит уже со смещением (`+05:00`), поэтому дописывать `Z` нельзя. */
    val generatedAt: String,
    val metrics: Map<AdminReportMetric, Int>
)

/** Точка графика: день и значение метрики за него. */
data class AdminReportSeriesPoint(val date: String, val value: Int)

/** Динамика одной метрики; дни без файла отчёта в точках отсутствуют. */
data class AdminReportSeries(
    val metric: AdminReportMetric,
    val points: List<AdminReportSeriesPoint>
)

/** Период графика. `All` — от самого старого отчёта до сегодня. */
enum class AdminReportPeriod(val title: String, val days: Int?) {
    Week("7 дней", 7),
    Month("30 дней", 30),
    Quarter("90 дней", 90),
    All("Всё время", null)
}

/** Чем закончилась постановка генерации: очередь, «файл уже есть» или ошибка. */
enum class AdminGenerateReportResult { Queued, Exists, Error }

/** Больше четырёх линий на одном графике уже не читаются — предел как в вебке. */
const val ADMIN_CHART_MAX_METRICS = 4
