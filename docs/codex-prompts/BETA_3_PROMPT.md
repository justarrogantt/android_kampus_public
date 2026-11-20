# Beta 3 prompt — отчёты о крашах от тестеров

```text
Прочитай AGENTS.md, CLAUDE.md, docs/PROJECT_MAP.md и app/build.gradle.kts.

Контекст.
Сборка для тестеров минифицирована R8 (betaRelease, isMinifyEnabled = true).
CI сохраняет mapping.txt артефактом на 90 дней, но отчётов о крашах нет:
Crashlytics не подключён. Краш у тестера мы узнаем, только если он сам напишет,
и стектрейс придёт скриншотом уже обфусцированным. Для первой раздачи это
главная дыра в обратной связи.

Firebase в проекте уже есть частично: плагин App Distribution, зависимости
firebase-appdistribution(-api) на beta и betaRelease, google-services.json
лежит по типам сборки — app/src/beta/ и app/src/betaRelease/. Плагина
google-services в build.gradle.kts при этом нет; разберись, как App
Distribution сейчас находит конфигурацию, прежде чем что-то добавлять.

Задача.
1. Подключи Firebase Crashlytics только к типам сборки beta и betaRelease.
   Debug и release остаются без него: debug-краши я вижу в logcat, а release
   тестерам не раздаётся.
2. Настрой автоматическую загрузку mapping.txt из R8 в Crashlytics, чтобы
   стектрейсы деобфусцировались сами. Артефакт mapping.txt в CI при этом
   оставь — он нужен как запасной путь.
3. Версии Crashlytics и плагина положи в gradle/libs.versions.toml рядом с
   остальными, не хардкодь в build.gradle.kts.
4. Проверь, что debug-сборка по-прежнему собирается без google-services.json:
   сейчас это осознанно так, и обратно ломать нельзя.
5. Добавь тестовый краш, доступный только в beta/betaRelease, чтобы можно было
   один раз убедиться, что отчёты доходят. Спрячь его так, чтобы тестер не
   наткнулся случайно, и опиши мне, как его вызвать.
6. Проверь, не нужно ли новое правило в app/proguard-rules.pro (файл сейчас
   на 15 строк).

Ограничения.
- Никакой аналитики и сбора поведения: только краши. В теле запросов ходят
  личные данные, и расширять телеметрию я не просил.
- BASE_URL, applicationId, уровни SDK и подпись не трогай.

Проверка.
./gradlew :app:assembleStagingBetaRelease
./gradlew :app:assembleStagingDebug

После изменений обнови docs/PROJECT_MAP.md.
```
