# Task 1 prompt

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
