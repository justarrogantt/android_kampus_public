# Add this block to AGENTS.md

Copy this section into the end of the existing `AGENTS.md`.

```md
---

# Admin logic implementation rules

The current uploaded project launches `AdminPanelScreen` from `MainActivity`.

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

After every meaningful code change, update:

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
```
