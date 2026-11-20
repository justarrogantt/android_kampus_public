# Admin logic implementation roadmap

This roadmap is designed for safe step-by-step work with Codex.

Main rule: Codex should complete one stage, compile-check, update docs, then stop. Do not ask it to implement the whole admin panel logic in one prompt.

## Stage 0 — Documentation and rules

Goal: make Codex maintain project knowledge while working.

Tasks:

- Create/update `docs/PROJECT_MAP.md`.
- Add admin logic rules to `AGENTS.md`.
- Correct outdated `AGENTS.md` statements about the current app entry point.

Do not change app behavior in this stage.

## Stage 1 — Add admin state layer without changing behavior

Goal: introduce ViewModel architecture while keeping the same UI and mock data.

Create:

```text
feature/admin/ui/AdminUiState.kt
feature/admin/ui/AdminEvent.kt
feature/admin/ui/AdminViewModel.kt
feature/admin/ui/AdminViewModelFactory.kt
```

Initial state should contain:

```text
currentTab
activeDialog
users
roles
permissions
usersSearchQuery
rolesSearchQuery
permissionsSearchQuery
isLoading
isRefreshing
isSaving
errorMessage
```

At the end of this stage, UI can still look and behave almost the same, but `AdminPanelScreen` should accept state/events or get a ViewModel.

## Stage 2 — Move tab/dialog/search state out of composables

Goal: make UI mostly stateless.

Move these states from composables into `AdminUiState`:

- current tab;
- active dialog;
- users search query;
- roles search query;
- permissions search query;
- selected user role in user settings dialog;
- selected role to assign to user;
- selected permission to add to role;
- form states for role and permission dialogs.

The UI should call events like:

```text
AdminEvent.TabSelected
AdminEvent.SearchUsersChanged
AdminEvent.UserClicked
AdminEvent.RoleClicked
AdminEvent.PermissionClicked
AdminEvent.DialogClosed
```

## Stage 3 — Add fake admin repository

Goal: replace `AdminMockData` direct usage with an in-memory repository.

Create:

```text
feature/admin/domain/repository/AdminRepository.kt
feature/admin/data/repository/FakeAdminRepository.kt
```

The fake repository should support:

- get users;
- get roles;
- get permissions;
- create/update/soft-delete/hard-delete/restore role;
- create/update/soft-delete/hard-delete/restore permission;
- get user roles;
- attach role to user;
- soft/hard detach role from user;
- restore user role;
- get role permissions;
- attach permission to role;
- detach permission from role.

At this stage everything should work locally without backend.

## Stage 4 — Wire all UI actions to fake logic

Goal: every visible button should perform a local action.

Implement through ViewModel events:

- Refresh data.
- Create role.
- Update role.
- Soft delete role.
- Hard delete role.
- Restore role.
- Create permission.
- Update permission.
- Soft delete permission.
- Hard delete permission.
- Restore permission.
- Assign role to user.
- Soft detach role from user.
- Hard detach role from user.
- Restore user role.
- Attach permission to role.
- Detach permission from role.

Keep fake logic simple but realistic.

## Stage 5 — Add UI operation states

Goal: the UI should properly show operation state.

Add support for:

- loading initial data;
- refreshing;
- saving;
- deleting;
- empty list state;
- error messages;
- validation errors.

Minimum validation:

- name is not blank;
- slug is not blank;
- slug contains only lowercase letters, numbers, hyphen and underscore;
- duplicate slug is rejected in fake repository.

## Stage 6 — Add admin DTOs, mappers and Retrofit service

Goal: add backend contracts but do not switch UI to remote yet.

Create:

```text
feature/admin/data/dto/AdminRoleDto.kt
feature/admin/data/dto/AdminPermissionDto.kt
feature/admin/data/dto/AdminUserDto.kt
feature/admin/data/dto/UserRoleLinkDto.kt
feature/admin/data/dto/AdminPayloadDtos.kt
feature/admin/data/dto/SuccessMessageDto.kt
feature/admin/data/mapper/AdminMappers.kt
feature/admin/data/remote/AdminService.kt
```

Add `createAdminService(...)` to `data/network/di/ApiServiceFactory.kt` using the same authorized client setup as `createAuthorizedService(...)`.

Do not duplicate token-refresh logic.

## Stage 7 — Add remote admin repository

Goal: implement backend calls behind the existing `AdminRepository` interface.

Create:

```text
feature/admin/data/repository/RemoteAdminRepository.kt
```

Map DTOs to app models. Convert network errors into readable messages.

Keep `FakeAdminRepository` available for preview/local fallback.

## Stage 8 — Switch ViewModel to remote repository

Goal: use backend data in the real app.

Update `AdminViewModelFactory` to construct:

```text
AdminService -> RemoteAdminRepository -> AdminViewModel
```

Keep previews independent from the network by providing fake state/sample data to preview composables.

## Stage 9 — Auth/admin routing decision

Goal: decide whether app should start directly from admin or require auth first.

Possible options:

1. Keep `MainActivity -> AdminPanelScreen` for lab/demo.
2. Restore `RegistrationFlowScreen`, then navigate to admin after successful authorization.
3. Add a small root component that observes auth state and routes to registration/admin.

Do this only after admin logic is stable.

## Stage 10 — Final cleanup

Goal: polish and document.

Tasks:

- Remove direct usage of `AdminMockData` from runtime code.
- Keep sample data only for previews/fake repository.
- Update `docs/PROJECT_MAP.md`.
- Update `AGENTS.md` only with reusable rules, not task history.
- Run compile/build locally.
