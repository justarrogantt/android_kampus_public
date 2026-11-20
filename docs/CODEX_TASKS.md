# Codex tasks for admin logic

Use these tasks one by one. Do not give Codex the whole roadmap at once.

Every task must end with:

- compile check if possible;
- updated `docs/PROJECT_MAP.md`;
- concise summary of changed files.

## Task 0 — Add project map and update AGENTS rules

### Prompt for Codex

```text
Read AGENTS.md and inspect the current Android project.

Task:
1. Create docs/PROJECT_MAP.md if it does not exist.
2. Fill it with the current project structure and admin UI file map.
3. Update AGENTS.md with a rule that docs/PROJECT_MAP.md must be updated after every meaningful code change.
4. Correct the outdated AGENTS.md statement that MainActivity launches RegistrationFlowScreen. In the current project MainActivity launches AdminPanelScreen.
5. Do not change app behavior.

Validation:
- Do not edit Kotlin files unless needed for comments/import cleanup.
- Do not change Gradle files.
- Report exactly what was changed.
```

### Review checklist

- `docs/PROJECT_MAP.md` exists.
- `AGENTS.md` no longer misleads Codex about the current entry point.
- No app behavior changed.

---

## Task 1 — Add AdminUiState/AdminEvent/AdminViewModel skeleton

### Prompt for Codex

```text
Read AGENTS.md and docs/PROJECT_MAP.md.

Task:
Add a first admin state layer without changing the visual design.

Create these files:
- app/src/main/java/com/example/ysukampus/feature/admin/ui/AdminUiState.kt
- app/src/main/java/com/example/ysukampus/feature/admin/ui/AdminEvent.kt
- app/src/main/java/com/example/ysukampus/feature/admin/ui/AdminViewModel.kt
- app/src/main/java/com/example/ysukampus/feature/admin/ui/AdminViewModelFactory.kt

For now, use the existing AdminUser/AdminRole/AdminPermission/AdminTab/AdminDialogState models from ui/admin.

AdminUiState should include at minimum:
- currentTab
- activeDialog
- users
- roles
- permissions
- usersSearchQuery
- rolesSearchQuery
- permissionsSearchQuery
- isLoading
- isRefreshing
- isSaving
- errorMessage

AdminEvent should include at minimum:
- TabSelected
- RefreshClicked
- AddClicked
- DialogClosed
- UserClicked
- RoleClicked
- PermissionClicked
- UsersSearchChanged
- RolesSearchChanged
- PermissionsSearchChanged
- ErrorShown

AdminViewModel should initialize state from current MockUsers/MockRoles/MockPermissions and handle only simple UI events for now.

Do not connect backend.
Do not implement delete/create/update operations yet.
Do not redesign UI.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- New ViewModel/state/event files exist.
- No network code added.
- Runtime still can use mock data.
- `PROJECT_MAP.md` updated.

---

## Task 2 — Wire AdminPanelScreen to AdminViewModel

### Prompt for Codex

```text
Read AGENTS.md and docs/PROJECT_MAP.md.

Task:
Wire AdminPanelScreen to the new AdminViewModel while preserving the current UI.

Requirements:
1. AdminPanelScreen should collect AdminUiState from AdminViewModel.
2. currentTab and activeDialog should come from AdminUiState, not local rememberSaveable state.
3. Header tab selection, add button, refresh button, card clicks and dialog close should dispatch AdminEvent.
4. Keep screen previews working by either providing preview state or keeping overloads that accept explicit state/event lambdas.
5. Do not connect backend.
6. Do not implement CRUD behavior yet.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- `AdminPanelScreen` no longer owns `currentTab` and `activeDialog` locally for runtime.
- Previews still work.
- Visual design unchanged.
- `PROJECT_MAP.md` updated.

---

## Task 3 — Move list search state to ViewModel

### Prompt for Codex

```text
Read AGENTS.md and docs/PROJECT_MAP.md.

Task:
Move users/roles/permissions search state from composables into AdminUiState/AdminViewModel.

Requirements:
1. UsersTabContent, RolesTabContent and PermissionsTabContent should receive query and onQueryChange from parent instead of owning rememberSaveable search state.
2. Filtering can stay inside the tab composable or be moved to ViewModel, but query source must be AdminUiState.
3. Dispatch UsersSearchChanged, RolesSearchChanged, PermissionsSearchChanged events.
4. Keep previews working.
5. Do not connect backend.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- No search `rememberSaveable` remains inside list tab composables.
- Search state survives as ViewModel state.
- `PROJECT_MAP.md` updated.

---

## Task 4 — Add fake AdminRepository

### Prompt for Codex

```text
Read AGENTS.md and docs/PROJECT_MAP.md.

Task:
Introduce a fake repository for admin data and operations.

Create:
- app/src/main/java/com/example/ysukampus/feature/admin/domain/repository/AdminRepository.kt
- app/src/main/java/com/example/ysukampus/feature/admin/data/repository/FakeAdminRepository.kt

Use existing UI models for now: AdminUser, AdminRole, AdminPermission.

Repository should support suspend functions for:
- getUsers
- getRoles
- getPermissions
- createRole
- updateRole
- softDeleteRole
- hardDeleteRole
- restoreRole
- createPermission
- updatePermission
- softDeletePermission
- hardDeletePermission
- restorePermission
- attachRoleToUser
- softDetachRoleFromUser
- hardDetachRoleFromUser
- restoreUserRole
- attachPermissionToRole
- detachPermissionFromRole
- getRolePermissions

Fake repository should be in-memory and initialized from MockUsers/MockRoles/MockPermissions.

Update AdminViewModel to load data from AdminRepository instead of directly from Mock* values.

Do not connect backend.
Do not remove AdminMockData yet because previews/fake repository may still use it.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- ViewModel depends on repository interface, not direct mock lists.
- Fake repository is in-memory.
- No Retrofit added yet.
- `PROJECT_MAP.md` updated.

---

## Task 5 — Wire user role actions locally

### Prompt for Codex

```text
Read AGENTS.md and docs/PROJECT_MAP.md.

Task:
Make UserSettingsDialog functional using AdminViewModel and FakeAdminRepository.

Requirements:
1. Move selected existing role and selected new role state out of UserSettingsDialog into AdminUiState/AdminViewModel.
2. Add events for:
   - UserExistingRoleSelected
   - UserNewRoleSelected
   - AssignRoleToUserClicked
   - SoftDetachUserRoleClicked
   - HardDetachUserRoleClicked
   - RestoreUserRoleClicked if the UI supports restore
3. Wire buttons in UserSettingsDialog to these events.
4. Update users list after changes.
5. Show simple errorMessage in state when action is invalid, for example assigning no selected role or duplicate role.
6. Keep visuals close to current design.
7. Do not connect backend.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- User role assignment works locally.
- User role detach buttons are not empty handlers anymore.
- Dialog state is ViewModel-owned.
- `PROJECT_MAP.md` updated.

---

## Task 6 — Wire role CRUD and role-permission actions locally

### Prompt for Codex

```text
Read AGENTS.md and docs/PROJECT_MAP.md.

Task:
Make RoleSettingsDialog and AddRoleDialog functional using AdminViewModel and FakeAdminRepository.

Requirements:
1. Move role form fields to AdminUiState/AdminViewModel.
2. Add events for:
   - RoleNameChanged
   - RoleSlugChanged
   - RoleDescriptionChanged
   - CreateRoleClicked
   - UpdateRoleClicked
   - SoftDeleteRoleClicked
   - HardDeleteRoleClicked
   - RestoreRoleClicked if needed
   - RolePermissionSelected
   - AttachPermissionToRoleClicked
   - DetachPermissionFromRoleClicked
3. Wire dialog buttons to events.
4. Add simple validation:
   - name not blank
   - slug not blank
   - slug matches lowercase letters/numbers/hyphen/underscore
   - duplicate slug rejected by fake repository
5. Update roles list after changes.
6. Keep visual design unchanged.
7. Do not connect backend.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- Add role creates a visible new role locally.
- Update role changes list data locally.
- Soft/hard delete actions work locally.
- Role permission add/detach works locally.
- `PROJECT_MAP.md` updated.

---

## Task 7 — Wire permission CRUD locally

### Prompt for Codex

```text
Read AGENTS.md and docs/PROJECT_MAP.md.

Task:
Make AddPermissionDialog and PermissionSettingsDialog functional using AdminViewModel and FakeAdminRepository.

Requirements:
1. Move permission form fields to AdminUiState/AdminViewModel.
2. Add events for:
   - PermissionNameChanged
   - PermissionSlugChanged
   - PermissionDescriptionChanged
   - CreatePermissionClicked
   - UpdatePermissionClicked
   - SoftDeletePermissionClicked
   - HardDeletePermissionClicked
   - RestorePermissionClicked if needed
3. Wire dialog buttons to events.
4. Add same simple validation as roles.
5. Update permissions list after changes.
6. Keep visual design unchanged.
7. Do not connect backend.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- Add permission creates a visible new permission locally.
- Update permission changes list data locally.
- Delete actions work locally.
- `PROJECT_MAP.md` updated.

---

## Task 8 — Add loading/error/empty/saving states

### Prompt for Codex

```text
Read AGENTS.md and docs/PROJECT_MAP.md.

Task:
Add proper operation states to the admin UI.

Requirements:
1. Use AdminUiState fields for loading, refreshing, saving/deleting and errorMessage.
2. Disable action buttons while saving/deleting.
3. Show empty state cards when filtered lists are empty.
4. Show a simple dismissible error message/snackbar/card for errors.
5. Keep current visual style.
6. Do not connect backend.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- UI handles loading/error/empty states.
- Buttons cannot be double-clicked while operations run.
- `PROJECT_MAP.md` updated.

---

## Task 9 — Add admin DTOs, mappers and Retrofit service

### Prompt for Codex

```text
Read AGENTS.md, docs/PROJECT_MAP.md and docs/API_MAPPING_ADMIN.md.

Task:
Add backend API contracts for the admin panel, but do not switch the UI to remote data yet.

Create DTOs, mappers and AdminService based on docs/API_MAPPING_ADMIN.md.

Requirements:
1. Use kotlinx.serialization and @SerialName for snake_case fields.
2. Retrofit paths must not start with /api because BASE_URL is expected to end with /api/.
3. Add createAdminService(...) to ApiServiceFactory.kt using the same authorized OkHttp setup as createAuthorizedService(...).
4. Do not duplicate token refresh logic.
5. Do not change AdminViewModel to use the remote repository yet.
6. Keep FakeAdminRepository as active.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- DTOs compile.
- Retrofit methods match API mapping.
- Authorized client includes device id, auth token and token authenticator.
- UI still uses fake repository.
- `PROJECT_MAP.md` updated.

---

## Task 10 — Add RemoteAdminRepository but keep fake active

### Prompt for Codex

```text
Read AGENTS.md, docs/PROJECT_MAP.md and docs/API_MAPPING_ADMIN.md.

Task:
Implement RemoteAdminRepository behind the existing AdminRepository interface.

Requirements:
1. RemoteAdminRepository should call AdminService.
2. Map DTOs to app models through mapper functions.
3. Convert common exceptions into readable messages using existing project error helpers where possible.
4. Do not switch AdminViewModelFactory to remote yet.
5. Keep FakeAdminRepository as the active runtime repository.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
```

### Review checklist

- Remote repository exists and implements the same interface as fake.
- DTO mapping is centralized.
- UI still works with fake.
- `PROJECT_MAP.md` updated.

---

## Task 11 — Switch runtime admin data to remote repository

### Prompt for Codex

```text
Read AGENTS.md, docs/PROJECT_MAP.md and docs/API_MAPPING_ADMIN.md.

Task:
Switch AdminViewModelFactory runtime repository from FakeAdminRepository to RemoteAdminRepository.

Requirements:
1. Construct AdminService through createAdminService(...).
2. Construct RemoteAdminRepository.
3. Keep previews and design previews using fake/sample data, not network.
4. Ensure loading/error states are used around remote operations.
5. Keep token refresh/auth behavior centralized in existing network auth stack.
6. Do not change BASE_URL or local.properties.

After the code changes, update docs/PROJECT_MAP.md.

Validation:
Run ./gradlew :app:compileDebugKotlin if possible.
Then test manually against backend.
```

### Review checklist

- Runtime uses backend.
- Previews do not hit network.
- 401 refresh remains centralized.
- Common errors display readable messages.
- `PROJECT_MAP.md` updated.

---

## Task 12 — Final cleanup

### Prompt for Codex

```text
Read AGENTS.md and docs/PROJECT_MAP.md.

Task:
Clean up the admin logic implementation.

Requirements:
1. Remove unused imports and dead code.
2. Ensure runtime code no longer directly reads MockUsers/MockRoles/MockPermissions except fake repository/previews.
3. Keep design tokens and UI style unchanged.
4. Ensure docs/PROJECT_MAP.md accurately reflects final file structure.
5. Add TODOs only for real remaining work.
6. Do not do large architecture rewrites.

Validation:
Run:
- ./gradlew :app:compileDebugKotlin
- ./gradlew :app:assembleDebug
```

### Review checklist

- Project compiles locally.
- Runtime data flow is clear.
- Documentation is up to date.
- No direct API calls in composables.
