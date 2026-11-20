# Beta 8 prompt — обновление приложения со своего сервера (Android + CI)

```text
Прочитай AGENTS.md, CLAUDE.md, docs/PROJECT_MAP.md и docs/BETA_READINESS.md
(раздел про 403 в App Distribution).

Зачем.
Firebase App Distribution в проекте перестал отдавать бинарь: метаданные релиза
приходят, права исправны (Accepted, событие Downloaded записывается), а
скачивание отвечает 403 всем и по любому пути — тестерам, владельцу проекта
через кнопку Download в консоли и даже APK, залитому в консоль вручную мимо CI.
Причина не на нашей стороне. Раздача переезжает на свой бэкенд.

Бэкенд уже написан, влит и развёрнут на проде: /home/sakura/backend,
мерж-коммит da5b530 в ветке notifications (её и крутит прод),
app/routers/builds.py, app/services/build_service.py,
app/schemas/build_schemas.py. Метаданные лежат JSON-файлом рядом с APK, без
таблицы и миграции.

Ручки живые, проверены 2026-09-24 запросами к проду:

    GET  /api/builds/beta/latest  -> 404 {"detail":"Бета-сборок пока нет"}
    POST /api/builds/beta         -> 403 (с неверным secret_key)

То есть код доехал и секрет подхватился; 404 здесь — правильный ответ, сборок
ещё ни одной не залито.

Контракт (вычитан из кода, не из описания).

POST /api/builds/beta — заливка из CI, multipart:
  apk           файл
  version_code  int, 1..2_100_000_000
  version_name  str, ^[0-9A-Za-z._+-]{1,64}$ — без пробелов и слешей
  notes         str, необязательно, до 10000
  secret_key    str, секрет в поле формы, не в query
  Ответы: 200 — сохранено, 403 — неверный ключ, 409 — этот version_code уже
  залит с другим файлом, 413 — больше MAX_BUILD_SIZE_MB (сейчас 200),
  422 — не APK или невалидные поля.

GET /api/builds/beta/latest — без авторизации, Cache-Control: no-cache.
  {version_code, version_name, notes, size, sha256, url}
  404, если сборок нет вообще.

GET /api/builds/beta/download?version_code=N — без авторизации, отдаёт APK.
  Без параметра — последняя.

`url` в ответе относительный: "/api/builds/beta/download?version_code=N".

Задача.

1. Шаг в .github/workflows/firebase-app-distribution.yml: после сборки залить
   APK на бэкенд. Секрет — из secrets.BUILD_UPLOAD_SECRET (имя переменной на
   бэкенде такое же), адрес — из уже имеющегося secrets.BASE_URL. version_code и
   version_name берутся из тех же VERSION_CODE/VERSION_NAME, что уже заданы в env
   workflow. Заливка не должна валиться молча: ненулевой код ответа обязан ронять
   шаг. 409 при перезапуске того же прогона — не ошибка, если файл тот же;
   разберись по ответу бэкенда и обработай осмысленно.
   Выгрузку в App Distribution пока оставь: пусть раздача идёт в оба места, пока
   новый путь не проверен на живых тестерах.

2. Клиент манифеста. Ручка без авторизации, поэтому она идёт в PublicService
   (createPublicAuthService), а не в авторизованный клиент и уж точно не в
   четвёртый — см. раздел про три клиента в CLAUDE.md.

3. BetaUpdateManager переводится с Firebase на этот манифест
   (ui/main/update/BetaUpdateManager.kt). Обновление есть, если
   manifest.version_code > BuildConfig.VERSION_CODE. UI уведомления уже есть и
   не меняется — меняется только источник данных. Вход тестера через браузер
   уходит совсем: гугловый аккаунт для обновления больше не нужен.

4. Скачивание и установка. Качать по url из манифеста, проверять sha256 до
   установки — он для того и отдаётся. Ставить через PackageInstaller
   (FileProvider не нужен). В AndroidManifest.xml добавится
   REQUEST_INSTALL_PACKAGES, сейчас его нет. На MIUI установка требует
   подтверждения пользователем и разрешения «установка из этого источника» —
   предусмотри отказ и внятное сообщение, а не тихое ничего.

5. Firebase App Distribution SDK убирается из зависимостей
   (firebase-appdistribution и firebase-appdistribution-api в app/build.gradle.kts,
   плагин, InstallActivity в манифесте). ВАЖНО: Crashlytics и google-services
   остаются, их не трогать — они не про раздачу и работают.

6. BuildConfig.APP_DISTRIBUTION_ENABLED сейчас означает «сборка выгружается в App
   Distribution». После переезда смысл другой — переименуй под новое значение
   (например BETA_UPDATES_ENABLED) и оставь true у beta и betaRelease.

Ловушки, проверенные руками.

- Относительный url склеивай через resolveApiFileUrl
  (data/network/ApiUrls.kt): он берёт схему и хост из BASE_URL и приклеивает
  путь. Наивная склейка с BASE_URL, который сам кончается на /api/, даст
  /api/api/. Заодно это уважает переключатель серверов: манифест приедет с того
  стенда, который выбран в приложении.
- 404 от latest означает «раздача не настроена», а не «обновлений нет». Это
  разные состояния, и в UI их путать нельзя.
- version_name в CI сейчас 0.1.0-beta.<номер>, под паттерн подходит. Если кто-то
  задаст versionName с пробелом, бэкенд ответит 422 — шаг CI должен упасть
  громко, а не проглотить.
- Тихие отказы в этом проекте уже дважды стоили двух недель: пустой секрет
  BETA_KEY_PASSWORD выключил подпись, сравнение BUILD_TYPE со строкой "beta"
  выключило проверку обновления. Любое новое условие должно падать или писать в
  лог, а не молча выключать функцию.

Ограничения.
- Не заводи четвёртый Retrofit-клиент.
- Сетевых вызовов внутри композаблов нет.
- Тексты интерфейса — на русском, в res/values/strings.xml.
- @Preview для новых состояний.
- BASE_URL, applicationId, уровни SDK и подпись не трогай.

Проверка.
./gradlew :app:compileStagingDebugKotlin
./gradlew :app:testStagingDebugUnitTest
./gradlew :app:assembleStagingBetaRelease

Секрет уже заведён, просить его не надо: BUILD_UPLOAD_SECRET лежит в GitHub
Secrets Android-репозитория и в .env прода, значения совпадают.

Стенд один. На ysukampus-test ни секрета, ни кода builds нет, и CI заливает по
адресу из secrets.BASE_URL, то есть только на прод. Это осознанно на время
беты. Следствие, которое надо учесть в клиенте: тестер, переключившийся в
приложении на тестовый сервер, получит от latest 404 — и это не поломка, а
«на этом стенде раздача не настроена». Не показывай это как ошибку обновления.

После изменений обнови docs/PROJECT_MAP.md и docs/BETA_READINESS.md.
```
