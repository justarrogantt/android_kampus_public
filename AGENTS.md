# AGENTS.md

Operational guide for coding agents working in this repository.

## Project Purpose

`ysukampus` is a single-module Android app (`:app`) written in Kotlin with Jetpack Compose. The current working line launches the Telegram-based authentication/registration flow from `MainActivity` via `RegistrationFlowScreen`. The mobile admin panel UI remains implemented under `ui/admin`, but it is not the current launcher root. Older local refs (`lab1`, `origin/lab1`) launch a Retrofit demo screen for three info endpoints (`info/client`, `info/database`, `info/server`) with request/DTO error simulation.

## Repo Lines

- Current branch: `role_policy`, tracking `origin/role_policy` when this file was updated.
- `role_policy`: current admin panel UI under `ui/admin`, with auth/registration code retained under `feature/registration`.
- `lab2`: auth/registration implementation history.
- `origin/lab2`: earlier auth/registration implementation; API code was still under `data/network/ApiService.kt` and did not include the newer authorized-session/token-refresh split.
- `main` and `lab1`: lab/demo implementation with `MainApp`, `MainScreen`, `MainViewModel`, `MainRepository`, and `data/ApiService.kt`.
- Commands and Gradle tooling are effectively shared across these refs: one Android app module, Kotlin/Compose, Gradle wrapper.
- When a task names a branch/ref, inspect that ref with `git show <ref>:<path>` or `git grep <pattern> <ref>` instead of assuming current `lab2` paths exist there.

## Important Directories

- `app/src/main/java/com/example/ysukampus/` - app source.
- `app/src/main/java/com/example/ysukampus/MainActivity.kt` - only launcher activity; calls `RegistrationFlowScreen`.
- `app/src/main/java/com/example/ysukampus/ui/admin/` - current admin panel UI, mock data, dialogs, list screens, and reusable admin components.
- `app/src/main/java/com/example/ysukampus/feature/registration/` - retained registration/auth state machine, screens, components, domain interactor, and `RegistrationViewModelFactory`.
- `app/src/main/java/com/example/ysukampus/data/auth/` - Retrofit service interfaces, service builders, repositories, token storage interface.
- `app/src/main/java/com/example/ysukampus/data/network/` - DTOs, auth state, interceptors, token refresh authenticator, error parsing.
- `app/src/main/java/com/example/ysukampus/data/local/` - encrypted token storage and device id storage.
- `app/src/main/java/com/example/ysukampus/data/` - used on `main`/`lab1` for the older Retrofit info demo; mostly absent from current `lab2` except DTO history in remote refs.
- `app/src/main/java/com/example/ysukampus/ui/theme/` - Compose Material theme and local font families.
- `app/src/main/res/values/` - strings, colors, Android theme.
- Root `.codex` is currently a managed local file in this workspace, so Codex subagents should be installed in `~/.codex/agents/` instead of `.codex/agents/` unless the local setup changes.
- `docs/PROJECT_MAP.md` - current project structure and admin UI file map; update it after every meaningful code change.
- `docs/CODEX_SUBAGENTS.md` - reusable Codex task templates for common `ysukampus` workflows; consult it for recurring Android/Compose/admin/build/review tasks.
- `gradle/libs.versions.toml` - version catalog for AGP/Kotlin/AndroidX/Compose BOM.

## Codex Subagents

- Installed Codex subagents for this project currently live in global `~/.codex/agents/` because root `.codex` in this workspace is a managed file, not a directory.
- Future Codex runs should proactively choose and use these subagents when the task matches, even if the user did not explicitly name a subagent.
- Prefer one best-fit subagent first; combine multiple only when the task clearly spans multiple concerns.
- Keep subagent use scoped and purposeful. Do not delegate trivial edits that can be completed safely in the current agent.
- For recurring task shapes, consult `docs/CODEX_SUBAGENTS.md` and reuse or adapt the closest template instead of re-inventing task framing from scratch.

### Recommended routing

- `search-specialist` - use first for fast repository reconnaissance, symbol/file discovery, or narrowing a large search space before deeper work.
- `code-mapper` - use before non-trivial changes when the execution path, ownership boundary, state flow, or blast radius is still unclear.
- `kotlin-specialist` - use for Kotlin, coroutines, sealed UI state, DTO/serialization, ViewModel, repository, and Android/JVM contract work.
- `mobile-developer` - use for Android app behavior involving lifecycle, screen flow, navigation, permissions, storage, network interruptions, and device-specific UX constraints.
- `mobile-app-developer` - use for broader mobile product flows spanning multiple screens, app state restoration, release-sensitive behavior, or cross-feature UX reliability.
- `ui-designer` - use when a task needs implementation-ready UI decisions from screenshots/design docs before writing Compose code.
- `ui-fixer` - use for tightly scoped visual/layout/state fixes where the issue is already identified and the smallest safe UI patch is desired.
- `build-engineer` - use for Gradle, flavors, build variants, signing, CI, dependency resolution, compiler/build pipeline issues, or flaky build diagnosis.
- `test-automator` - use when adding or repairing automated tests, targeted regression coverage, fixtures, or test harness support around changed behavior.
- `reviewer` - use for correctness/security/regression review of a diff or planned change, especially before commit or handoff.
- `documentation-engineer` - use when updating repository docs so they stay aligned with current code paths, commands, and operator workflows.

### Project-specific routing hints

- For admin-panel tasks under `ui/admin`, start with `code-mapper` if the touched screen/state/repository path is not obvious, then use `kotlin-specialist` or `ui-fixer` depending on whether the change is logic-heavy or presentation-heavy.
- For Compose work driven by `design/` screenshots/specs, prefer `ui-designer` before implementation when layout or interaction details are ambiguous.
- For ViewModel/repository/network/auth stack changes under `data/auth`, `data/network`, `data/session`, and `feature/registration`, prefer `kotlin-specialist`; add `mobile-developer` when lifecycle or degraded-network behavior matters.
- For `MainActivity`, navigation/root-flow, beta update flow, or cross-screen behavior, prefer `mobile-app-developer`.
- For Gradle/build-type/flavor/signing/Firebase App Distribution issues, prefer `build-engineer`.
- For any meaningful code change that should add or adjust tests, use `test-automator` before finishing.
- Before final handoff for medium or high-risk changes, use `reviewer`.
- When updating `AGENTS.md`, `docs/PROJECT_MAP.md`, roadmap/checklist docs, or task prompts, prefer `documentation-engineer`.

### Delegation examples

- "Find the owning files for this admin screen bug" -> `search-specialist` or `code-mapper`
- "Fix a broken Compose dialog layout from screenshots" -> `ui-fixer`
- "Implement or debug ViewModel/repository/state logic" -> `kotlin-specialist`
- "Investigate lifecycle, flow, or mobile UX regressions" -> `mobile-developer` or `mobile-app-developer`
- "Diagnose Gradle assemble/compile/signing failure" -> `build-engineer`
- "Add regression coverage for this bugfix" -> `test-automator`
- "Review the diff before commit" -> `reviewer`
- "Update project docs to match reality" -> `documentation-engineer`

## Entry Points

- Android launcher: `MainActivity`.
- Compose root: `RegistrationFlowScreen(appContext = applicationContext)`.
- ViewModel: `RegistrationViewModel`.
- Manual dependency wiring: `RegistrationViewModelFactory`.
- Retrofit builders: `createPublicAuthService`, `createDeviceAuthService`, `createAuthorizedService` in `data/network/di/ApiServiceFactory.kt`.
- API base URL: `BuildConfig.BASE_URL`, generated from `BASE_URL` in root `local.properties`.
- Older `main`/`lab1` entry points: `MainActivity -> MainApp -> MainScreen`, with `MainViewModelFactory` constructing `MainRepository(Api.retrofitService)`.
- Older `main`/`lab1` base URL is hard-coded in `app/src/main/java/com/example/ysukampus/data/ApiService.kt` as `http://localhost:8000/`.

## Setup

- Use the checked-in Gradle wrapper: `./gradlew`.
- Required local config is `local.properties`; current local file contains `sdk.dir=/home/sakura/Android/Sdk` and `BASE_URL=http://127.0.0.1:8000/api/`.
- Do not commit `local.properties`; it is machine-local and already described by its header as Android Studio generated.
- Beta builds use a dedicated signing key in CI through `BETA_KEYSTORE_FILE`, `BETA_KEYSTORE_PASSWORD`, `BETA_KEY_ALIAS`, and `BETA_KEY_PASSWORD`. Without those env vars, local beta builds fall back to the machine-specific debug keystore.
- `BASE_URL` must be a valid Retrofit base URL and should keep the trailing slash. Empty or malformed values will break service creation.
- If the app talks to a backend running on the development machine while using an Android emulator/device and `BASE_URL` remains `http://127.0.0.1:8000/api/`, use port forwarding such as `adb reverse tcp:8000 tcp:8000` where appropriate.
- On `main`/`lab1`, there is no `BuildConfig.BASE_URL`; update the hard-coded `BASE_URL` in `data/ApiService.kt` only if the task explicitly targets that line.

## Commands

- List Gradle tasks: `./gradlew :app:tasks --all`
- Build staging debug APK: `./gradlew :app:assembleStagingDebug`
- Build release APK: `./gradlew :app:assembleRelease`
- Full Gradle build/check: `./gradlew :app:build`
- Kotlin compile/typecheck, staging debug: `./gradlew :app:compileStagingDebugKotlin`
- Kotlin compile/typecheck, dev debug: `./gradlew :app:compileDevDebugKotlin`
- Kotlin compile/typecheck, release: `./gradlew :app:compileReleaseKotlin`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- All unit tests: `./gradlew :app:test`
- Android instrumentation tests on connected device: `./gradlew :app:connectedDebugAndroidTest`
- Lint default variant: `./gradlew :app:lint`
- Lint debug: `./gradlew :app:lintDebug`
- Install debug on connected device: `./gradlew :app:installDebug`

No separate ktlint, detekt, Spotless, npm, or non-Gradle typecheck tooling is configured in the repository.

## Safe Change Rules

- Keep changes scoped to `:app` unless Gradle/tooling changes are explicitly needed.
- After every meaningful code change, update `docs/PROJECT_MAP.md` so it reflects new, changed, moved, or deleted files and the current implemented feature status.
- First identify the target repo line. Do not apply `lab2` auth conventions to `main`/`lab1` demo files unless the task is a migration.
- Preserve the manual DI shape in `RegistrationViewModelFactory` unless replacing it deliberately across the app.
- When adding API calls, place Retrofit interface methods in the matching service type:
  - public unauthenticated code/status calls in `PublicService`;
  - device-id auth/refresh/confirmation calls in `AuthService`;
  - bearer-token protected calls in `AuthorizedService`.
- Protected requests currently require both `X-Device-Id` and `Authorization`; keep `DeviceIdInterceptor` before `AuthInterceptor` in the authorized client unless there is a reason to change request headers.
- Token refresh is centralized in `TokenAuthentificator`; avoid duplicating refresh logic in screens or repositories.
- Keep token persistence behind `TokenStorage`; current production implementation is `EncryptedPrefsTokenStorage`.
- Keep device identity behind `DeviceIdStorage`; current implementation stores `device_id` in `app_prefs`.
- Do not casually rename existing `Authentification*` classes/methods. The spelling is used consistently in current files and a rename would be broad churn.
- UI text belongs in `res/values/strings.xml` when user-facing; preserve existing Russian copy unless the task asks for copy changes.
- Reuse existing Compose typography/resources (`Unbounded`, `Onest`, `JetBrainsMono`, colors in `colors.xml`) for registration UI changes.
- In `main`/`lab1`, preserve the explicit simulation behavior in `MainViewModel`: request-error mode throws `IOException`, DTO-error mode throws `SerializationException`, and successful responses are pretty-printed with `Json`.

## Local Conventions

- Kotlin code style is `official` in `gradle.properties`.
- Java/Kotlin target compatibility is Java 11.
- Compose is enabled; UI is implemented with composable functions and Material 3.
- DTOs use `kotlinx.serialization` and `@SerialName` for snake_case backend fields.
- Repositories parse some raw `ResponseBody` manually because endpoints can return different bodies/status codes.
- Auth status polling recognizes `"pending"` and `"success"` only; unknown statuses become `RegistrationUiState.Error`.
- `RegistrationUiState` is the screen-routing model for the retained auth flow.
- `main`/`lab1` use a separate `MainUiState` and do not have the auth/session state model.

## Expected Validation Workflow

For most code changes:

1. Run `./gradlew :app:compileStagingDebugKotlin` for the default staging line, or the matching flavor-specific compile task for the target variant.
2. Run the matching flavor-specific unit test task, such as `./gradlew :app:testStagingDebugUnitTest`.
3. Run the matching flavor-specific lint task, such as `./gradlew :app:lintStagingDebug`, when touching Android/Compose/resources/manifest.
4. Run `./gradlew :app:assembleStagingDebug` before handing off UI, manifest, networking, or Gradle changes unless the task targets another variant.
5. Run the matching connected test task, such as `./gradlew :app:connectedStagingDebugAndroidTest`, only when the change needs device/emulator validation.

For auth/backend behavior, also validate manually against a configured backend because the repository only contains placeholder example tests.

## Project-Specific Pitfalls

- `BuildConfig.BASE_URL` comes from `local.properties`; CI or a fresh checkout may have an empty value unless configured.
- The manifest currently allows cleartext traffic (`android:usesCleartextTraffic="true"`) and has `INTERNET` permission.
- `SessionManager.restoreSession()` treats the session as authorized only when both access and refresh tokens exist.
- Logout and logout-all clear local tokens and then request a fresh auth code after a 2 second demo delay.
- `ApiErrorParser.toReadableMessage()` consumes `errorBody().string()`; avoid trying to read the same response body twice.
- `RegistrationViewModel` owns polling via a single `pollingJob`; cancel it before starting a new code request.
- `TokenAuthentificator` uses `runBlocking` and a `Mutex` inside OkHttp authenticator code; keep refresh calls minimal and non-recursive.
- `createAuthorizedService` creates its refresh service with the device-id client, not the authorized client, to avoid refresh recursion.
- `FirstRegistrationScreen`, `SecondRegistrationScreen`, and role/group selection assets exist, but the current launcher flow routes through Telegram confirmation and authorized actions.
- In `main`/`lab1`, the base URL is `http://localhost:8000/`; that is different from current `lab2` local `BASE_URL=http://127.0.0.1:8000/api/`.
- The checked-in tests are Android Studio examples only; they do not cover auth, repositories, or Compose screens.

## Ambiguities Found

- App scope: strings mention schedule and role/group selection, but `MainActivity` currently launches the Telegram registration/session flow. The admin panel is implemented but not the current launcher root.
- Branch scope: `lab1` is a Retrofit info demo while `lab2` is auth/registration history; the active product line is `role_policy`.
- Backend contract: endpoint paths and statuses are inferred from Retrofit interfaces and hard-coded ViewModel logic; no API spec is checked in.
- Local backend addressing: `local.properties` uses `127.0.0.1:8000`; on emulator/device this likely needs `adb reverse` or a different host depending on runtime target.
- UI state `Idle` exists but is not currently reached by `RegistrationViewModel`; treat it as unused unless wiring changes.

## Ask A Human When

- Changing backend endpoints, response shapes, auth status values, or token refresh semantics.
- Deciding whether the active product flow should include role/group selection screens or bypass them as it does now.
- Changing `BASE_URL`, cleartext traffic policy, package/application id, SDK versions, or signing/release behavior.
- Replacing manual DI with a framework or changing token/device-id storage.
- Modifying user-visible Russian copy when product intent is unclear.
- Existing uncommitted changes overlap files you need to edit and ownership is unclear.

## Self-improvement rule

At the end of each task, consider whether AGENTS.md should be updated.

Only update AGENTS.md when the task revealed reusable project knowledge that will help future work.

Good updates:
- project architecture decisions;
- recurring user preferences;
- exact build/test commands;
- important folder responsibilities;
- coding conventions;
- design system rules.

Bad updates:
- task history;
- temporary TODOs;
- bug descriptions that were already fixed;
- long explanations;
- duplicated or obvious rules.

Keep AGENTS.md concise. Prefer modifying existing bullets instead of adding new sections.

---

# YSU Kampus Android UI design task

This project includes a mobile admin panel UI based on the provided website screenshots and frontend source.

## Design reference files

Read these files before making UI changes:

@./design/DESIGN_FROM_WEB.md
@./design/THEME_TOKENS.md
@./design/COMPONENTS_SPEC.md
@./design/SCREEN_SPEC.md
@./design/IMPLEMENTATION_PLAN.md
@./design/BACKEND_CONTEXT.md

## Screenshots reference

Use screenshots from:

@./design/screenshots/

The screenshots are the primary visual reference. The frontend source and backend context are secondary references.

## Main implementation goal

Implement the provided admin panel screens as a normal Android mobile application using:

- Kotlin
- Jetpack Compose
- Material 3

The task is UI-only. Do not implement real backend integration, authorization, networking, database, or API calls.

Use fake local sample data for previews and screen states.

## Required screens

Implement all screens described in `SCREEN_SPEC.md`:

- Users list
- User settings modal
- User role assignment dropdown state
- User role selected state
- Roles list
- Role settings screen
- Add role screen
- Permissions list
- Add permission screen
- Permission settings screen

## UI rules

- Use Jetpack Compose only.
- Do not create XML layouts.
- Create reusable components for:
  - top header
  - segmented tabs
  - search field
  - list cards
  - status badge
  - modal bottom/sheet panel
  - primary button
  - soft delete button
  - hard delete button
  - dropdown selector
  - form text field
- Keep the UI close to the screenshots but adapt it as a proper Android app.
- Use `dp` for layout sizes and `sp` for text.
- Extract colors, typography, radius, spacing and shadows into theme/design token files.
- Add `@Preview` for every screen and important component.
- Do not change unrelated project files.

## Backend context

Backend entities are described in `BACKEND_CONTEXT.md`.

Use this context only to understand the meaning of users, roles, permissions, soft delete, hard delete, restore, assign role, and assign permission.

Do not implement API calls.

# Admin logic implementation rules

The current uploaded project launches `RegistrationFlowScreen` from `MainActivity`; admin UI is implemented but not the launcher root.

Admin UI is implemented in:

@./app/src/main/java/com/example/ysukampus/ui/admin/

Admin design references are in:

@./design/

Admin implementation documentation is in:

@./docs/PROJECT_MAP.md
@./docs/LOGIC_IMPLEMENTATION_ROADMAP.md
@./docs/API_MAPPING_ADMIN.md
@./docs/CODEX_TASKS.md
@./docs/CODEX_REVIEW_CHECKLIST.md

## Documentation rule

After every meaningful code change, agents must update:

@./docs/PROJECT_MAP.md

Update it with:

- new files;
- changed files;
- moved/deleted files;
- implemented feature status;
- real remaining TODOs.

Do not duplicate source code in docs. Keep docs concise and useful.

## Admin architecture rules

- Use Kotlin + Jetpack Compose + Material 3.
- Do not create XML layouts.
- Do not put backend calls inside composables.
- Composables should render state and emit events.
- ViewModels should handle UI events and expose state.
- Repositories should handle data operations.
- Keep fake repository available until remote repository is fully working.
- Keep previews independent from network.
- Do not change the admin visual design while adding logic unless explicitly asked.

## Admin networking rules

The current Android network stack already has:

- `AuthInterceptor` for bearer token;
- `DeviceIdInterceptor` for `X-Device-Id`;
- `TokenAuthentificator` for token refresh;
- `EncryptedPrefsTokenStorage` for token persistence.

Admin protected requests must reuse this stack.

Do not duplicate token refresh logic in repositories, ViewModels, or composables.

## Retrofit path rule

Existing auth endpoints use paths like `auth/code`, which means `BASE_URL` is expected to end with `/api/`.

For admin endpoints, use Retrofit paths like:

- `ref/user/`
- `ref/policy/role`
- `ref/policy/permission`

Do not add `/api/` to Retrofit annotations unless `BASE_URL` is deliberately changed.

## Validation

After most Kotlin changes, run:

```bash
./gradlew :app:compileDebugKotlin
```

Before handing off larger changes, run:

```bash
./gradlew :app:assembleDebug
```

If Gradle cannot run because dependencies are unavailable, state that clearly and still update docs.
