# Beta 1 prompt — мерж кэша в develop и сборка тестерам

```text
Прочитай AGENTS.md, CLAUDE.md и docs/PROJECT_MAP.md.

Контекст.
Ветка feat/room-cache содержит пять коммитов локального кэша на Room (Room +
KSP, ResponseCache/ScheduleCache/ForumCache, общий loadCached, кэш расписания,
шторки «Профиль», ленты форума, уведомлений и треда поста) и ещё около сорока
коммитов, которых нет в develop.

CI (.github/workflows/firebase-app-distribution.yml) собирает и заливает бету
в Firebase App Distribution только на push в develop и
feature/firebase-app-distribution. Пока ветка не в develop, тестеры получают
сборку без кэша.

Задача.
1. Смержи feat/room-cache в develop. Мерж-коммит, не rebase: в ветке уже есть
   опубликованная история.
2. Разреши конфликты, если будут. Перед этим посмотри, что именно разошлось, —
   не принимай «свою» сторону вслепую.
3. После мержа проверь, что собирается именно тот вариант, который уходит
   тестерам, а не только debug.
4. Сверь docs/PROJECT_MAP.md с фактическим состоянием develop после мержа:
   таблица статусов фич и раздел «Last updated» должны описывать то, что
   реально в ветке.
5. Не пушь ничего, пока я не подтвержу: покажи diffstat мержа и результат
   сборки, и дождись ответа. Push в develop запускает раздачу тестерам.

Ограничения.
- Не меняй BASE_URL, applicationId, уровни SDK и подпись.
- Историю develop не переписывай.

Проверка.
./gradlew :app:compileStagingBetaReleaseKotlin
./gradlew :app:testStagingDebugUnitTest
./gradlew :app:assembleStagingBetaRelease
```
