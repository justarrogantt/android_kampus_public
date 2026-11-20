# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`AGENTS.md` — подробное операционное руководство (роутинг Codex-субагентов, бриф по дизайну, история веток). Здесь только то, что нужно в повседневной работе; за деталями — в `AGENTS.md` и `docs/PROJECT_MAP.md`. Учти, что часть `AGENTS.md` написана под старые ветки: проверяй пути по рабочему дереву, прежде чем им доверять.

## Команды

Один Gradle-модуль `:app`, используем вложенный wrapper. Два флейвора (`dev`, `staging`) × типы сборки (`debug`, `beta`, `release`), поэтому почти всем задачам нужно имя с флейвором.

```bash
./gradlew :app:compileStagingDebugKotlin
```

```bash
./gradlew :app:assembleStagingDebug
```

```bash
./gradlew :app:testStagingDebugUnitTest
```

```bash
./gradlew :app:lintStagingDebug
```

Запуск одного теста: `./gradlew :app:testStagingDebugUnitTest --tests "com.example.ysukampus.ExampleUnitTest.addition_isCorrect"`. В репозитории лежат только шаблонные тесты Android Studio — auth, репозитории и Compose-экраны не покрыты, поведение бэкенда проверяется вручную.

Флейвор `dev` переключает `BuildConfig.AUTH_MODE` на `"dev"` и зашивает `DEV_USER_ID`/`DEV_ROLE` и прочее, из-за чего приложение уходит на `DevLoginScreen` вместо Telegram-авторизации. `beta` — отлаживаемый тип сборки, который CI выгружает в Firebase App Distribution (`assembleStagingBeta appDistributionUploadStagingBeta`); подписывается ключом из `BETA_KEYSTORE_*`, а локально молча откатывается на debug-keystore.

Ни ktlint, ни detekt, ни Spotless не подключены. Стиль Kotlin — `official`, таргет Java 11.

### local.properties

`BASE_URL` (обязательно с хвостовым `/api/`) и необязательный `TELEGRAM_BOT_LINK` читаются из корневого `local.properties` в `BuildConfig`. Отладочный переключатель серверов берёт адреса из `BASE_URL_TEST`/`BASE_URL_PROD`, а ботов — из `TELEGRAM_BOT_LINK_TEST`/`TELEGRAM_BOT_LINK_PROD` (без них обе ветки падают обратно на `BASE_URL`/`TELEGRAM_BOT_LINK`). В свежем клоне `BASE_URL` пустой, и создание Retrofit-сервисов падает в рантайме. Для эмулятора, ходящего в бэкенд на хосте: `adb reverse tcp:8000 tcp:8000`.

## Архитектура

Kotlin + Compose + Material 3, Retrofit/OkHttp/kotlinx.serialization, ручной DI (без Hilt/Dagger). XML-layout'ов нет.

### Навигация — это состояние, а не Navigation-Compose

`MainActivity` → `RegistrationFlowScreen`, который ветвится через `when` по `RegistrationViewModel.registrationUiState` (sealed interface `RegistrationUiState`). После перехода в `AuthorizedActions` рисуется `MainScreen` (оболочка с нижними вкладками Home / Schedule / Forum / Profile, вкладка хранится в `rememberSaveable`) либо `AdminPanelScreen` — по локальному булеву `showAdminPanel`. NavHost'а нет нигде: добавить экран — значит добавить состояние или вкладку, а не маршрут.

`SessionManagerProvider` — синглтон на процесс, хранящий `SessionManager.authState`. `RegistrationFlowScreen` следит за ним и, если состояние переходит в `NotAuthorized`, пока пользователь находится в авторизованной зоне, закрывает админку и вызывает `onSessionExpired()` — именно так 401 из любого места возвращает пользователя на регистрацию.

### Три Retrofit-клиента, три уровня авторизации

Все собираются в `data/network/di/ApiServiceFactory.kt`, у каждого свой стек OkHttp. Новый эндпоинт кладём в сервис, соответствующий его уровню авторизации, а не заводим четвёртый клиент:

- `PublicService` (`createPublicAuthService`) — без интерсепторов; неаутентифицированные вызовы кода/статуса.
- `AuthService` (`createDeviceAuthService`) — только `DeviceIdInterceptor`; auth/refresh/подтверждение.
- `AuthorizedService` / `AdminApi` (`createAuthorizedService`, `createAdminApi`) — `DeviceIdInterceptor`, затем `AuthInterceptor`, плюс `TokenAuthentificator` как OkHttp-аутентификатор.

Обновление токена централизовано в `TokenAuthentificator` (внутри кода аутентификатора используются `runBlocking` и `Mutex`), а о неудаче он сообщает через `SessionManager::onNotAuthorized`. Его refresh-сервис строится на **device-id**-клиенте, а не на авторизованном — так избегается рекурсия, это надо сохранять. Не дублируй логику refresh в репозиториях, ViewModel'ях и композаблах.

Токены живут за `TokenStorage` (`EncryptedPrefsTokenStorage`), идентичность устройства — за `DeviceIdStorage` (`SharedPrefsDeviceIdStorage`, ключ `device_id` в `app_prefs`). Пути Retrofit задаются относительно базового URL, заканчивающегося на `/api/`: пиши `ref/user/`, `auth/code`, но не `/api/...`.

DTO используют `kotlinx.serialization` и `@SerialName` для snake_case-полей; общая конфигурация `Json` — `ignoreUnknownKeys = true, explicitNulls = false`. Некоторые репозитории разбирают сырой `ResponseBody` вручную, потому что тело ответа зависит от статуса. `ApiErrorParser.toReadableMessage()` вычитывает `errorBody().string()` — прочитать тело второй раз уже нельзя.

### Админ-панель

`ui/admin` — самодостаточная MVVM-фича: `AdminViewModel` + `AdminUiState`/`AdminEvent`, данные приходят из `AdminRepository`, который выбирается в `AdminRepositoryProvider` по явному `AdminDataSourceMode` (`Fake` — для превью и разработки, `Remote` — `RemoteAdminRepository` + `AdminApi`). `FakeAdminRepository` должен оставаться рабочим, а `@Preview` — независимыми от сети. Вкладка модерации дополнительно поднимает `ModeratorWebSocketClient` (URL выводится из `BuildConfig.BASE_URL`, токен — из `TokenStorage`) только пока эта вкладка открыта; события `new_post`/`new_comment` показывают пилюлю ручного обновления и не должны сами вставлять элементы в список.

### Темы

Сосуществуют две темы: `YsukampusTheme` (обычная Material 3 с dynamic color) для регистрации и основной оболочки и `YsuAdminTheme` с самописными `YsuAdminColors` и объектами токенов в `ui/theme/YsuAdminTokens.kt` — для админки. Цвета основной оболочки — в `ui/main/MainColors.kt` (`KampusColors`). Шрифты (`Unbounded`, `Onest`, `JetBrainsMono`) лежат в `res/font` и отдаются через `ui/theme/Type.kt`; переиспользуй их, а не заводи новые семейства. Иконки профиля поставляются как SVG в `app/src/main/assets/profile_icons_svg` рядом с векторными drawable.

## Соглашения

- Тексты интерфейса — на русском и живут в `res/values/strings.xml`; не переводи существующие строки без отдельной просьбы.
- Композаблы отрисовывают состояние и отправляют события; сетевых вызовов внутри композаблов нет. `@Preview` — для каждого экрана и значимого компонента.
- Не переименовывай существующее написание `Authentification*` — оно используется единообразно, а переименование затронет слишком много файлов.
- После каждого содержательного изменения кода обновляй `docs/PROJECT_MAP.md` (новые/изменённые/перемещённые файлы, статус фич, реальные TODO).
- Манифест разрешает cleartext-трафик; менять это, а также `BASE_URL`, applicationId, уровни SDK или подпись — решение человека.
