# Beta 4 prompt — хвост фазы 4: чужой профиль, аватар, анекдот

```text
Прочитай AGENTS.md, CLAUDE.md, docs/PROJECT_MAP.md и docs/CATCHUP_PLAN.md.
Это остаток фазы 4 — полный промпт фазы лежит в
docs/codex-prompts/CATCHUP_4_PROMPT.md, большая часть по нему уже сделана.

Что уже готово (не переделывай).
Живые имя и группа в профиле, шторка редактирования, своя активность через
forum/posts/me и forum/comments/me, кнопка «Открыть» со своей карточкой форума
(forum/user-profile + user-posts + user-comments), кэш этих ответов в
ForumRepository.

Что осталось.
1. Профиль другого пользователя. Тап по автору поста или комментария сейчас не
   ведёт никуда. Открывай тем же оверлеем, что и свою карточку, — по
   profile_id (UUID) через forum/user-profile, forum/user-posts,
   forum/user-comments. Прямой UI-референс — UserProfileOverlay в
   /home/sakura/frontend (react/src/pages/MainPage/UserProfileOverlay),
   контракт — react/src/pages/MainPage/services/profile.api.ts и
   /home/sakura/backend/app/routers/forum.py.
2. GET profile/avatar/{profile_id} — аватар произвольного пользователя в этом
   оверлее.
3. Аватар на весь экран по тапу, со скачиванием через GET
   profile/photo/download. Разрешение на запись в галерею уже объявлено в
   манифесте с maxSdkVersion=28 — посмотри, как сохранение сделано для картинок
   форума, и переиспользуй тот же путь.
4. Пункт «Расскажи анекдот» в ui/main/profile/ProfileScreen.kt (около строки
   176) висит на тосте «В разработке» — подключи GET profile/joke.
5. Пункт «Тема оформления» там же (около строки 182) — работа на отдельную
   фичу, к бете её не будет. Убери пункт из меню целиком, а не прячь за тостом:
   тестеру честнее не видеть кнопку, чем нажимать на заглушку. Зафиксируй это
   в PROJECT_MAP как отложенный TODO, а не как сделанное.

Ограничения.
- Навигации-Compose в проекте нет: оверлей чужого профиля — это состояние в
  оболочке, а не маршрут. Смотри, как сделан ForumUiState.openedPostId и как
  ведёт себя таббар при открытом треде.
- Карточки постов и комментариев переиспользуй из ui/main/forum/components,
  новые не пиши.
- Сетевых вызовов внутри композаблов нет.
- Кэш: чужой профиль через общий loadCached (data/local/db/CachedLoader.kt),
  как уже сделано для своей активности. TTL возьми такой же и объясни выбор
  в PROJECT_MAP.
- @Preview для новых экранов и компонентов, независимые от сети.
- Тексты интерфейса — на русском, в res/values/strings.xml.

Проверка.
./gradlew :app:compileStagingDebugKotlin
./gradlew :app:testStagingDebugUnitTest

После изменений обнови docs/PROJECT_MAP.md.
```
