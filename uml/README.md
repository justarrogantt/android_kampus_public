# UML-диаграммы YSU Kampus

Начните с [обзора архитектуры](01-foundations/01-system-overview.png).

В каталоге 35 диаграмм Android-приложения и процесса сборки на ветке `feature/self-hosted-beta-updates` (коммит `144c759`). Диаграммы охватывают основные типы, зависимости и сценарии. Бэкенд и внешние сервисы представлены через интерфейсы, которые использует приложение.

## Архитектура и общие компоненты

Обзор системы, запуск приложения, локальный кэш, выбор окружения и автоматические проверки.

| Диаграмма | Исходник | Изображение |
| --- | --- | --- |
| YSU Kampus — архитектура приложения | [PUML](01-foundations/01-system-overview.puml) | [PNG](01-foundations/01-system-overview.png) |
| Запуск приложения и выбор экрана | [PUML](01-foundations/02-startup-routing.puml) | [PNG](01-foundations/02-startup-routing.png) |
| Локальный кэш ответов | [PUML](01-foundations/03-room-cache.puml) | [PNG](01-foundations/03-room-cache.png) |
| Выбор окружения бэкенда | [PUML](01-foundations/04-server-switch.puml) | [PNG](01-foundations/04-server-switch.png) |
| Автоматические проверки в репозитории | [PUML](01-foundations/05-test-coverage.puml) | [PNG](01-foundations/05-test-coverage.png) |

## Вход, сессия и сетевой транспорт

Регистрация через Telegram и dev-вход, жизненный цикл сессии, HTTP-клиенты и общий WebSocket.

| Диаграмма | Исходник | Изображение |
| --- | --- | --- |
| Компоненты входа и регистрации | [PUML](02-auth-session/01-registration-components.puml) | [PNG](02-auth-session/01-registration-components.png) |
| Состояния входа и регистрации | [PUML](02-auth-session/02-login-states.puml) | [PNG](02-auth-session/02-login-states.png) |
| Обмен кода Telegram на сессию | [PUML](02-auth-session/03-telegram-code.puml) | [PNG](02-auth-session/03-telegram-code.png) |
| Восстановление и завершение сессии | [PUML](02-auth-session/04-session-restore.puml) | [PNG](02-auth-session/04-session-restore.png) |
| HTTP-клиенты и заголовки авторизации | [PUML](02-auth-session/05-network-clients.puml) | [PNG](02-auth-session/05-network-clients.png) |
| Общее обновление access-токена | [PUML](02-auth-session/06-token-refresh.puml) | [PNG](02-auth-session/06-token-refresh.png) |
| Общий транспорт WebSocket | [PUML](02-auth-session/07-websocket-transport.puml) | [PNG](02-auth-session/07-websocket-transport.png) |
| Локальные данные аккаунта и устройства | [PUML](02-auth-session/08-session-storage.puml) | [PNG](02-auth-session/08-session-storage.png) |

## Главная, расписание и профиль

Главная, учебное расписание, настройки аккаунта, профиль форума, оболочка и тема Compose.

| Диаграмма | Исходник | Изображение |
| --- | --- | --- |
| Главная · расписание, погода и форум | [PUML](03-campus/01-home.puml) | [PNG](03-campus/01-home.png) |
| Расписание · сущности и состав учебного дня | [PUML](03-campus/02-schedule-model.puml) | [PNG](03-campus/02-schedule-model.png) |
| Расписание · выбор группы и загрузка вкладки | [PUML](03-campus/03-schedule-loading.puml) | [PNG](03-campus/03-schedule-loading.png) |
| Профиль · настройки аккаунта | [PUML](03-campus/04-account-profile.puml) | [PNG](03-campus/04-account-profile.png) |
| Профиль форума · своя и чужая активность | [PUML](03-campus/05-forum-profile.puml) | [PNG](03-campus/05-forum-profile.png) |
| Оболочка · вкладки, слои и темы Compose | [PUML](03-campus/06-compose-theme.puml) | [PNG](03-campus/06-compose-theme.png) |

## Форум и уведомления

Лента, обсуждения, публикация, работа с изображениями и уведомления.

| Диаграмма | Исходник | Изображение |
| --- | --- | --- |
| Форум · лента, тред и пагинация | [PUML](04-communication/01-forum-feed-thread.puml) | [PNG](04-communication/01-forum-feed-thread.png) |
| Форум · публикация поста или комментария | [PUML](04-communication/02-forum-publication.puml) | [PNG](04-communication/02-forum-publication.png) |
| Изображения · просмотр и сохранение | [PUML](04-communication/03-image-viewer-gallery.puml) | [PNG](04-communication/03-image-viewer-gallery.png) |
| Уведомления · список, счётчики и WebSocket | [PUML](04-communication/04-notifications-channels.puml) | [PNG](04-communication/04-notifications-channels.png) |
| Уведомления · переход к комментарию | [PUML](04-communication/05-notification-thread.puml) | [PNG](04-communication/05-notification-thread.png) |

Персональный канал уведомлений использует [общий транспорт WebSocket](02-auth-session/07-websocket-transport.png).

## Администрирование

Вкладки админ-панели, доступы, история, логи, статистика и модерация форума.

| Диаграмма | Исходник | Изображение |
| --- | --- | --- |
| Админ-панель и источники данных | [PUML](05-admin/01-admin-panel.puml) | [PNG](05-admin/01-admin-panel.png) |
| Пользователи, роли и права | [PUML](05-admin/02-users-roles-permissions.puml) | [PNG](05-admin/02-users-roles-permissions.png) |
| История изменений доступов | [PUML](05-admin/03-access-history.puml) | [PNG](05-admin/03-access-history.png) |
| Журнал серверных HTTP-запросов | [PUML](05-admin/04-request-logs.puml) | [PNG](05-admin/04-request-logs.png) |
| Статистика и генерация дневного отчёта | [PUML](05-admin/05-report-generation.puml) | [PNG](05-admin/05-report-generation.png) |
| Модерация форума по HTTP | [PUML](05-admin/06-moderation-http.puml) | [PNG](05-admin/06-moderation-http.png) |
| Живое обновление очереди модерации | [PUML](05-admin/07-moderation-live.puml) | [PNG](05-admin/07-moderation-live.png) |

Канал модерации использует [общий транспорт WebSocket](02-auth-session/07-websocket-transport.png).

## Сборка и доставка

Варианты сборки, Crashlytics, публикация бета-версии и установка обновлений.

| Диаграмма | Исходник | Изображение |
| --- | --- | --- |
| Варианты сборки и Crashlytics | [PUML](06-delivery/01-build-variants.puml) | [PNG](06-delivery/01-build-variants.png) |
| Сборка и публикация бета-APK в CI | [PUML](06-delivery/02-beta-publication.puml) | [PNG](06-delivery/02-beta-publication.png) |
| Проверка наличия бета-обновления | [PUML](06-delivery/03-beta-update-check.puml) | [PNG](06-delivery/03-beta-update-check.png) |
| Загрузка и установка бета-APK | [PUML](06-delivery/04-beta-installation.puml) | [PNG](06-delivery/04-beta-installation.png) |

## Пересборка изображений

Понадобятся PlantUML, Java, Graphviz (`dot`) и шрифт DejaVu Sans. Выполните команду из корня репозитория.

```bash
JAVA_TOOL_OPTIONS=-Djava.awt.headless=true plantuml -charset UTF-8 -failfast2 'uml/**/*.puml'
```

PlantUML сохранит каждый PNG рядом с соответствующим PUML.
