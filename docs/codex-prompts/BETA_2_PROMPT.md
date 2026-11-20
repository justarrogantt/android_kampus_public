# Beta 2 prompt — переключатель серверов в CI-сборке

```text
Прочитай AGENTS.md, CLAUDE.md, docs/PROJECT_MAP.md и
app/src/main/java/com/example/ysukampus/data/network/ApiEnvironment.kt.

Контекст (баг подтверждён чтением кода, не догадка).
Коммит 863188b добавил переключатель тестового и продового бэкенда, включённый
в том числе в betaRelease — сборке, которая уходит тестерам. В сборке из CI он
не появляется вообще.

Почему. Шаг «Create local.properties» в
.github/workflows/firebase-app-distribution.yml пишет только две строки:

    echo "BASE_URL=${{ secrets.BASE_URL }}" >> local.properties
    echo "TELEGRAM_BOT_LINK=https://t.me/ysukampusbot" >> local.properties

А app/build.gradle.kts (около строки 50) без BASE_URL_TEST/BASE_URL_PROD роняет
оба адреса обратно на BASE_URL. Дальше ApiEnvironmentHolder.isSwitchAvailable
требует Test.baseUrl != Prod.baseUrl, получает false, и ServerSwitcher не
рисует ничего ни на экране регистрации, ни в профиле. То же с
TELEGRAM_BOT_LINK_TEST / TELEGRAM_BOT_LINK_PROD: у тестового и продового
бэкендов разные боты, код одного второй не примет.

Задача.
1. Допиши шаг «Create local.properties» четырьмя строками:
   BASE_URL_TEST, BASE_URL_PROD, TELEGRAM_BOT_LINK_TEST, TELEGRAM_BOT_LINK_PROD
   — каждая из своего секрета репозитория. Имена секретов выбери по образцу
   существующего BASE_URL и перечисли их мне в ответе: заводить их в настройках
   репозитория буду я руками.
2. Сделай так, чтобы отсутствие этих секретов было видно, а не проглатывалось.
   Сейчас пустой секрет молча превращается в «переключателя нет». Достаточно
   шага, который после генерации local.properties печатает, различаются ли
   адреса, — но реальные URL в лог не выводи, это адреса стендов.
3. Плашка версии в ui/main/profile/ProfileScreen.kt (около строки 209) жёстко
   зашита как "v0.1 beta", хотя CI проставляет versionName
   0.1.0-beta.<номер запуска>. Тестер не может сказать, какая у него сборка.
   Покажи в плашке BuildConfig.VERSION_NAME. Тап по ней открывает админку —
   это поведение сохрани.
4. Проверь, что в release-сборке переключатель по-прежнему выключен:
   SERVER_SWITCH_ENABLED там false, и ApiEnvironmentHolder обязан ходить на
   BuildConfig.BASE_URL.

Ограничения.
- Сам BASE_URL, applicationId, уровни SDK и подпись не трогай.
- Реальные адреса стендов в репозиторий не коммить: только имена секретов.
- Тексты интерфейса — на русском, в res/values/strings.xml.

Проверка.
./gradlew :app:compileStagingBetaReleaseKotlin

Локально переключатель можно проверить только через local.properties с разными
BASE_URL_TEST и BASE_URL_PROD — покажи мне, какие строки туда положить.

После изменений обнови docs/PROJECT_MAP.md.
```
