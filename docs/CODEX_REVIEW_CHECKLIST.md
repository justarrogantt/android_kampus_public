# Codex review checklist

Use this checklist after every Codex task.

## 1. Documentation

- [ ] Did Codex update `docs/PROJECT_MAP.md`?
- [ ] Did Codex describe newly created files?
- [ ] Did Codex remove or update descriptions of files it moved/deleted?
- [ ] Did Codex avoid turning `PROJECT_MAP.md` into a huge changelog?
- [ ] Did Codex update `AGENTS.md` only for reusable rules, not task history?

## 2. Architecture

- [ ] No API calls inside composables.
- [ ] No business logic inside composables.
- [ ] UI reads state and emits events.
- [ ] ViewModel handles events.
- [ ] Repository handles data operations.
- [ ] Remote repository, DTOs and mappers are separated.
- [ ] Token refresh logic is not duplicated.

## 3. Current project conventions

- [ ] Kotlin package names match project structure.
- [ ] Code uses Compose, not XML layouts.
- [ ] User-facing text remains Russian unless intentionally changed.
- [ ] Existing `YsuAdminTheme`, `YsuAdminColors`, `YsuAdminDimens`, `YsuAdminTextStyles` are reused.
- [ ] No unnecessary DI framework added.
- [ ] No unnecessary navigation library added unless task requires it.

## 4. Retrofit/API checks

- [ ] Retrofit admin paths do not start with `/api/` if `BASE_URL` ends with `/api/`.
- [ ] Retrofit paths do not start with `/` unless intentionally using absolute URL behavior.
- [ ] DTO snake_case fields use `@SerialName`.
- [ ] Nullable backend fields are nullable in DTOs.
- [ ] Protected requests use authorized OkHttp stack with device id and bearer token.
- [ ] 401 refresh remains inside `TokenAuthentificator`.

## 5. UI checks

- [ ] Design did not visually regress.
- [ ] Previews still compile.
- [ ] Loading state exists where async work exists.
- [ ] Error state is visible and dismissible.
- [ ] Empty state is shown for empty filtered lists.
- [ ] Buttons are disabled during save/delete where needed.
- [ ] Dialogs close only after successful operations unless intentionally kept open.

## 6. Fake repository checks

- [ ] Fake repository is in-memory.
- [ ] Fake repository does not call network.
- [ ] Fake repository validates duplicate slugs or duplicate relationships.
- [ ] Soft delete changes state rather than removing when appropriate.
- [ ] Hard delete removes item when appropriate.
- [ ] Restore clears deleted state when appropriate.

## 7. Build checks

Ask Codex or run locally:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

Optional:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

## 8. Red flags

Stop and ask for review if Codex:

- rewrites the whole UI without being asked;
- adds Hilt/Koin without approval;
- changes `BASE_URL` or `local.properties`;
- commits secrets, tokens, `.env`, `local.properties`;
- removes existing auth/token refresh classes;
- changes package/application id;
- moves many files in one task;
- edits Gradle versions without a clear reason;
- makes composables depend directly on Retrofit services;
- leaves placeholder click handlers after a logic task.
