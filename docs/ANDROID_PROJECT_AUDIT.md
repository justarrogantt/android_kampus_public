# Android project audit

This document describes the current state of the uploaded Android project `android-role_policy` and the safest strategy for adding admin-panel logic.

## Current app state

The project is a single-module Android app:

```text
:app
namespace/applicationId: com.example.ysukampus
UI: Kotlin + Jetpack Compose + Material 3
Networking: Retrofit + OkHttp + kotlinx.serialization
Auth storage: EncryptedSharedPreferences via security-crypto
DI: manual factories, no Hilt/Koin
```

## Important observation

`MainActivity.kt` currently launches:

```kotlin
AdminPanelScreen()
```

So the current uploaded project already starts from the admin-panel UI.

The top part of the existing `AGENTS.md` still says that `MainActivity` launches `RegistrationFlowScreen`. That is outdated for the uploaded project and should be corrected before Codex starts large logic work.

## Existing admin UI

Admin UI is already implemented under:

```text
app/src/main/java/com/example/ysukampus/ui/admin/
```

Important files:

| File | Current role |
|---|---|
| `AdminPanelScreen.kt` | Root admin screen. Holds current tab and active dialog in local `rememberSaveable` state. Uses mock data. |
| `AdminModels.kt` | UI models/enums: `AdminUser`, `AdminRole`, `AdminPermission`, `AdminTab`, `AdminDialogState`. |
| `AdminMockData.kt` | Hardcoded sample users, roles, permissions. |
| `screens/AdminTabContents.kt` | Users/Roles/Permissions/History list content with local search state. |
| `components/AdminComponents.kt` | Reusable visual components: header, tabs, cards, search, buttons, select, dialog frame, list items. |
| `dialogs/AdminDialogs.kt` | User, role, permission, add-role, add-permission dialogs. Form fields and selections are local state. Buttons currently mostly have empty handlers. |
| `AdminPreviews.kt` | Compose previews for screens/dialogs/components. |

## Existing theme/design system

Admin design tokens are implemented in:

```text
app/src/main/java/com/example/ysukampus/ui/theme/YsuAdminTokens.kt
```

This file contains:

- `YsuAdminColors`
- `YsuAdminDimens`
- `YsuAdminTextStyles`
- `YsuAdminTheme`

Keep these tokens. Do not rewrite the design while adding logic.

## Existing auth/network stack

The project already contains a working auth-oriented network foundation:

| File/area | Purpose |
|---|---|
| `data/auth/PublicService.kt` | Public auth endpoints. |
| `data/auth/AuthService.kt` | Device-id auth and refresh endpoints. |
| `data/auth/AuthorizedService.kt` | Existing protected auth/session endpoints. |
| `data/network/di/ApiServiceFactory.kt` | Retrofit/OkHttp creation. |
| `data/network/auth/AuthInterceptor.kt` | Adds `Authorization: Bearer ...`. |
| `data/network/auth/DeviceIdInterceptor.kt` | Adds `X-Device-Id`. |
| `data/network/auth/TokenAuthentificator.kt` | Refreshes tokens on 401. |
| `data/session/TokenStorage.kt` | Token storage abstraction. |
| `data/local/EncryptedPrefsTokenStorage.kt` | Current token storage implementation. |
| `data/session/SessionManager.kt` | Tracks auth state. |

Admin API should reuse the authorized OkHttp setup so protected requests get both:

```text
X-Device-Id
Authorization: Bearer <access_token>
```

## Current limitations before logic work

- Admin UI uses `MockUsers`, `MockRoles`, `MockPermissions` directly.
- No `AdminViewModel` exists.
- No `AdminUiState` or `AdminEvent` exists.
- Search state lives inside tab composables.
- Dialog form state lives inside dialog composables.
- Buttons for create/update/delete/assign actions are mostly placeholders.
- No admin Retrofit service/DTO/mappers exist yet.
- No `docs/PROJECT_MAP.md` exists in the uploaded project.

## Backend/API source

The web frontend project contains the admin API contract in:

```text
src/services/admin.api.ts
src/schemas/admin.schema.ts
```

Important Retrofit note:

The Android project reads `BASE_URL` from `local.properties`. Existing auth endpoints use paths like:

```kotlin
@POST("auth/code")
```

This suggests local `BASE_URL` is expected to end with `/api/`, for example:

```text
BASE_URL=http://127.0.0.1:8000/api/
```

Therefore Android admin Retrofit methods should use paths like:

```kotlin
@GET("ref/user/")
@GET("ref/policy/role")
```

Do not use leading `/api/...` in Retrofit annotations unless the project deliberately changes `BASE_URL`.

## Recommended strategy

Do not let Codex connect backend immediately.

Use this order:

1. Add/update project documentation and Codex rules.
2. Introduce admin `UiState`, `Event`, and `ViewModel` while still using fake/in-memory data.
3. Wire all UI buttons to ViewModel events.
4. Implement local fake logic for create/update/delete/restore/assign operations.
5. Add DTO/API/mappers separately and compile-check them.
6. Add `RemoteAdminRepository` separately.
7. Switch repository implementation from fake to remote only after UI logic is stable.
8. Add loading/error/empty/saving states.
9. Clean up and update `PROJECT_MAP.md`.

## Build validation status

A compile attempt in this sandbox could not finish because Gradle tried to download `gradle-9.2.1-bin.zip` and the sandbox has no internet access:

```text
java.net.UnknownHostException: services.gradle.org
```

So compile validation must be run locally in Android Studio/terminal:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```
