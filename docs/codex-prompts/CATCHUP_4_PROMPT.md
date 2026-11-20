# Catch-up phase 4 prompt — профиль: живые данные и публичный профиль

```text
Прочитай AGENTS.md, CLAUDE.md, docs/PROJECT_MAP.md и docs/CATCHUP_PLAN.md.

Зависимость: фаза 2 (картинки в форуме) желательна — карточки постов
переиспользуются в публичном профиле.

Контекст.
Экран профиля — единственный в основной оболочке, который так и не подключён
к бэкенду:
- ui/main/profile/ProfileUiState.kt: name = "Студент ЮГУ" и
  entityName = "Группа не указана" — жёстко зашитые строки. ProfileViewModel.init
  грузит только аватар.
- ui/main/profile/ProfileScreen.kt: пять действий висят на тосте
  "В разработке" — "Редактировать профиль", "Мои посты", "Расскажи анекдот",
  "Тема оформления" и кнопка в шапке.

Бэкенд тем временем перевёл ID пользователей форума на UUID ради приватности
телеграм-данных и открыл публичные профили. Локальные копии для сверки:
бэк — /home/sakura/backend (app/routers/profile.py, app/routers/forum.py),
веб-фронт — /home/sakura/frontend
(react/src/pages/MainPage/UserProfileOverlay — прямой UI-референс,
react/src/pages/MainPage/services/profile.api.ts — контракт).

Контракт:
- GET forum/user-profile — публичный профиль по profile_id
- GET forum/user-posts, GET forum/user-comments — чужая активность
- GET forum/posts/me, GET forum/comments/me — своя активность
- GET profile/avatar/{profile_id} — аватар произвольного пользователя
- GET profile/photo/download — скачивание своей фотографии
- GET profile/joke — анекдот
- Имя и группа пользователя приходят в UserDTO (см.
  /home/sakura/backend/app/schemas/auth_schemas.py)

Задача.
1. Живые данные профиля: ProfileViewModel грузит имя и entityName из UserDTO,
   ProfileUiState перестаёт держать заглушки-строки. Добавь состояния загрузки
   и ошибки вместо мгновенного показа фейка.
2. "Мои посты" -> экран своей активности: forum/posts/me и forum/comments/me,
   две вкладки. Веб переключается на вкладку комментариев, если постов ноль —
   повтори это поведение. Переиспользуй ThreadCard и CommentCard из
   ui/main/forum/components вместе с их лайками, а не пиши новые карточки.
3. Публичный профиль другого пользователя (оверлей поверх основной оболочки,
   как UserProfileOverlay в вебе): forum/user-profile + user-posts +
   user-comments. Открывается тапом по автору поста или комментария.
4. Кнопка "Открыть" рядом с "Редактировать профиль" — открывает свой публичный
   профиль тем же оверлеем.
5. Аватар на весь экран по тапу, со скачиванием через profile/photo/download.
6. "Расскажи анекдот" -> GET profile/joke.
7. "Редактировать профиль" и "Тема оформления" оставь на тосте "В разработке",
   но зафиксируй это в PROJECT_MAP как явный оставшийся TODO — не выдавай за
   готовое.

Ограничения.
- Навигации-Compose в проекте нет: публичный профиль и своя активность — это
  состояние в оболочке, а не маршрут. Смотри, как сделан ForumUiState.openedPostId.
- Сетевых вызовов внутри композаблов нет.
- @Preview для новых экранов и компонентов, независимые от сети.
- Тексты интерфейса — на русском, в res/values/strings.xml.

Проверка.
./gradlew :app:compileStagingDebugKotlin

После изменений обнови docs/PROJECT_MAP.md.
```
