# Catch-up phase 3 prompt — уведомления

```text
Прочитай AGENTS.md, CLAUDE.md, docs/PROJECT_MAP.md и docs/CATCHUP_PLAN.md.

Зависимость: фаза 1 (базовый WS-клиент с обработкой 1008) должна быть уже влита.

Контекст.
Бэкенд завёл подсистему уведомлений. Локальные копии для сверки контрактов:
бэк — /home/sakura/backend (app/routers/notifications.py,
app/services/notification_service.py, app/schemas/notification_schemas.py),
веб-фронт — /home/sakura/frontend
(react/src/pages/MainPage/services/notifications.api.ts — готовая zod-схема,
это самый точный источник формы ответа; UI-референс — шторка уведомлений
и NotificationsBottomSheet).

Контракт:
- GET notifications?limit&cursor -> { items, next_cursor, unread_count,
  moderation_count }. moderation_count у обычных пользователей всегда 0.
- POST notifications/read с телом {"notification_id": N} или {"all": true}
  -> { unread_count }
- WS /ws/user?token= : события notification_created (целиком карточка) и
  notification_target_unavailable { post_id, comment_id }
- Карточка: id (int), kind ("comment" | "reply"),
  actor { profile_id (uuid), display_name, display_color, avatar_url },
  target { post_id, comment_id, preview, image_preview_url, is_available,
  unavailable_reason: "comment_deleted" | "post_deleted" | null },
  created_at, read_at (nullable).

Задача.
1. Слой данных: data/network/dto/NotificationDtos.kt, методы в
   data/auth/AuthorizedService.kt, новый data/notifications/NotificationRepository.kt
   с курсорной пагинацией по образцу уже существующей ленты форума
   (data/forum/ForumRepository.kt).
2. WS: UserWebSocketClient на базовом клиенте из фазы 1, канал /ws/user.
   Живёт, пока пользователь в авторизованной зоне (MainScreen), а не пока
   открыта конкретная вкладка. notification_created добавляет карточку в
   начало списка и поднимает счётчик; notification_target_unavailable помечает
   уже загруженную карточку недоступной, а не удаляет её.
3. UI (новый пакет ui/main/notifications/): ModalBottomSheet со списком
   уведомлений, скелетон на первой загрузке, ретрай на ошибке, догрузка
   страниц по скроллу, "прочитать все", отметка одного прочитанным при
   открытии, пустое состояние. Недоступная цель не кликабельна и показывает
   причину.
4. Точки входа: кнопка уведомлений с бейджем непрочитанных — на экране
   расписания и в меню профиля (в вебе именно так). Отдельный бейдж модерации
   на пункте уведомлений, когда moderation_count > 0.
5. Тап по доступному уведомлению открывает тред поста и скроллит к
   комментарию (у ForumUiState уже есть openedPostId — переиспользуй его,
   не заводи параллельный механизм).
6. Тумблер дублирования в Telegram: PATCH profile/telegram-notifications,
   поле telegram_notifications_enabled приходит в UserDTO. В вебе тумблер
   живёт в редактировании профиля — если экрана редактирования у нас ещё нет
   (см. фазу 4), положи его в меню профиля и отметь это в PROJECT_MAP.

Ограничения.
- Не заводи четвёртый Retrofit-клиент: уведомления идут через AuthorizedService.
- Сетевых вызовов внутри композаблов нет.
- @Preview для новых компонентов, независимые от сети.
- Тексты интерфейса — на русском, в res/values/strings.xml.

Проверка.
./gradlew :app:compileStagingDebugKotlin

После изменений обнови docs/PROJECT_MAP.md.
```
