# План догоняния бэка и веб-фронта

Составлен 2026-09-03 после подтягивания `ysukampus/backend` (37 новых коммитов, HEAD `6f25ed4`) и `ysukampus/frontend` (81 новый коммит, HEAD `f4fbb3c`, версия веба `0.3.0`).

Локальные копии для сверки контрактов:

- бэкенд — `/home/sakura/backend`
- веб-фронт — `/home/sakura/frontend`

Android-приложение отстаёт от них по четырём крупным фичам и одному инфраструктурному багу. Ниже — фазы в порядке зависимостей; готовые промпты для отдельных чатов лежат в `docs/codex-prompts/CATCHUP_*_PROMPT.md`.

## Что появилось на бэке (сводка контрактов)

### Уведомления

| Метод | Путь | Заметки |
|---|---|---|
| `GET` | `notifications?limit&cursor` | `items`, `next_cursor`, `unread_count`, `moderation_count` |
| `POST` | `notifications/read` | тело `{"notification_id": N}` либо `{"all": true}` → `{"unread_count": N}` |
| `WS` | `/ws/user?token=` | `notification_created`, `notification_target_unavailable` |
| `PATCH` | `profile/telegram-notifications` | тумблер дублирования в Telegram-бота |

`UserDTO` получил поле `telegram_notifications_enabled`.

Форма карточки (`app/schemas/notification_schemas.py`, зеркало — `react/src/pages/MainPage/services/notifications.api.ts`): `id: int`, `kind: comment | reply`, `actor {profile_id (uuid), display_name, display_color, avatar_url}`, `target {post_id, comment_id, preview, image_preview_url, is_available, unavailable_reason: comment_deleted | post_deleted | null}`, `created_at`, `read_at`.

### Картинки в форуме

| Метод | Путь | Заметки |
|---|---|---|
| `POST` | `forum/media` | multipart `file` → `attachment_id` |
| `GET` | `forum/media/{attachment_id}?size=preview` | без `size` — оригинал |

`content` в `CreatePostRequest`/`CreateCommentRequest` стал необязательным, добавился `image_id`; пост без текста и без картинки отбивается валидатором. Ответы постов и комментариев обзавелись `image_url`, `image_preview_url`, `image_width`, `image_height`. Картинки проходят AI-модерацию.

### Публичный профиль (ID пользователей переведены на UUID)

| Метод | Путь |
|---|---|
| `GET` | `forum/user-profile` |
| `GET` | `forum/user-posts` |
| `GET` | `forum/user-comments` |
| `GET` | `forum/posts/me` |
| `GET` | `forum/comments/me` |
| `GET` | `profile/avatar/{profile_id}` |
| `GET` | `profile/photo/download` |
| `GET` | `profile/joke` |

### Расписание

`LessonSchema.lecturer_oid: str | null` и `AudienceSchema.members: [{label, name, oid}]` — чтобы «ПИ41б/1, ИВТ41б» разбиралось на отдельные тапабельные группы, а преподаватель открывал своё расписание. `oid = null` означает «в справочнике нет, тап не нужен».

### WebSocket

`app/routers/ws.py::reject()` теперь делает `accept()` и только потом `close(1008)`: раньше клиент видел только 1006 и не мог отличить протухший токен от обрыва сети. Добавился канал `/ws/user`, в канал модератора приехало событие `moderation_count` с живым размером очереди.

## Фазы

### Фаза 1 — WS-ядро и реакция на 1008

Инфраструктурная база для фазы 3. `ModeratorWebSocketClient` реконнектится с backoff, но на `1008` просто повторяет попытку с тем же протухшим токеном. Выносим общий базовый клиент, учим его обновлять токен при `1008`, добавляем событие `moderation_count`.

Затрагивает: `data/network/ws/`.

### Фаза 2 — Картинки в форуме

Загрузка вложения, `image_id` при создании поста и комментария, отрисовка картинки в `ThreadCard`/`CommentCard`, полноэкранный просмотрщик со скачиванием.

Затрагивает: `data/network/dto/ForumDtos.kt`, `data/auth/AuthorizedService.kt`, `data/forum/ForumRepository.kt`, `ui/main/forum/`.

Зависимостей нет — можно делать параллельно с фазой 1.

### Фаза 3 — Уведомления

Слой данных, персональный сокет, шторка со списком, счётчик непрочитанных, бейдж модерации, отметка прочитанного.

Затрагивает: новые `data/notifications/`, `data/network/dto/NotificationDtos.kt`, `data/network/ws/`, новые `ui/main/notifications/`.

Зависит от фазы 1 (базовый WS-клиент) и мягко от фазы 2 (`image_preview_url` в карточке уведомления).

### Фаза 4 — Профиль: живые данные и публичный профиль

Закрывает нынешние заглушки: `ProfileUiState.name`/`entityName` — жёстко зашитые строки, пять пунктов меню висят на тосте «В разработке».

Затрагивает: `data/auth/ProfileRepository.kt`, `ui/main/profile/`, новые `ui/main/profile/public/`.

Зависит от фазы 2 (карточки постов с картинками переиспользуются в публичном профиле).

### Фаза 5 — Расписание: кликабельные группы и преподаватели

`lecturer_oid` и `audience.members` в DTO и моделях, переход на чужое расписание, плашка «вернуться к своему» снизу.

Затрагивает: `data/network/dto/ScheduleDtos.kt`, `data/schedule/ScheduleMappers.kt`, `ui/main/schedule/`.

Зависимостей нет.

### Фаза 6 — Дизайн-полировка

Только после того, как функциональный разрыв закрыт. Веб успел уехать по мелочам: тосты ожидания при отправке поста и комментария, адаптивный размер названия пары, название выбранной темы вместо стрелки, аватар на весь экран, исправления line-height. Сверяться по `design/SCREEN_SPEC.md`, `design/THEME_TOKENS.md` и живому вебу.

## Порядок работы

Фазы 1, 2 и 5 независимы и могут идти параллельно в разных чатах. Фаза 3 ждёт первую, фаза 4 — вторую. Фаза 6 — последняя.
