# Android implementation plan

План для AI-агента в Android Studio. Выполнять по шагам, чтобы не сломать проект.

## Phase 0. Inspect project

1. Найти package name проекта.
2. Проверить, подключён ли Jetpack Compose.
3. Проверить, используется ли Material 3.
4. Найти существующие файлы темы: `Color.kt`, `Theme.kt`, `Type.kt` или аналоги.
5. Найти `MainActivity.kt` и текущий entry point.

Если Compose не подключён, не переписывать весь проект без необходимости. Сначала добавить минимальную Compose-конфигурацию или создать новые UI-файлы в уже существующем Compose-модуле.

## Phase 1. Add theme tokens

Создать или дополнить файлы темы:

```text
ui/theme/YsuAdminColors.kt
ui/theme/YsuAdminTypography.kt
ui/theme/YsuAdminTheme.kt
```

Использовать значения из `THEME_TOKENS.md`.

Do not hardcode colors in every component. Use tokens.

## Phase 2. Add models and mock data

Создать:

```text
ui/admin/AdminModels.kt
ui/admin/AdminMockData.kt
```

Models:

- `AdminUser`
- `AdminRole`
- `AdminPermission`
- `AdminTab`
- `AdminDialogState`
- `AdminStatus`

Mock data must cover all screenshots:

- user `Егор`, username `demo_user`;
- roles `Admin`, `User`, `Guest`;
- permissions like `get-list-user`, `read-user`, `create-user`, `update-user`, `delete-user`, `restore-user`, `get-list-role`, `read-role`, `create-role`, `create-permission`, `delete-log`.

## Phase 3. Build reusable components

Create components first, then screens.

Recommended files:

```text
ui/admin/components/AdminHeader.kt
ui/admin/components/AdminTabs.kt
ui/admin/components/AdminSearchField.kt
ui/admin/components/AdminCards.kt
ui/admin/components/AdminBadges.kt
ui/admin/components/AdminDialogFrame.kt
ui/admin/components/AdminFormFields.kt
ui/admin/components/AdminSelect.kt
ui/admin/components/AdminButtons.kt
ui/admin/components/AdminListItems.kt
```

Core components:

- `AdminHeader`
- `AdminTabHeader`
- `AdminSearchField`
- `AdminEntityCard`
- `AdminStatusBadge`
- `AdminRoleChip`
- `AdminDialogFrame`
- `AdminSheetBlock`
- `AdminSectionLabel`
- `AdminTextField`
- `AdminTextArea`
- `AdminSelect`
- `AdminPrimaryButton`
- `AdminActionButton`
- `RoleListItem`
- `PermissionListItem`

## Phase 4. Implement main tabs

Create:

```text
ui/admin/AdminPanelScreen.kt
ui/admin/screens/UsersTabContent.kt
ui/admin/screens/RolesTabContent.kt
ui/admin/screens/PermissionsTabContent.kt
ui/admin/screens/HistoryTabContent.kt
```

Requirements:

- `AdminPanelScreen` manages tab state and active dialog state.
- Each tab has local search state.
- Search filters mock lists.
- Cards open corresponding dialogs.
- Add button opens add dialogs only for roles and permissions.

## Phase 5. Implement dialogs

Create:

```text
ui/admin/dialogs/UserSettingsDialog.kt
ui/admin/dialogs/RoleSettingsDialog.kt
ui/admin/dialogs/AddRoleDialog.kt
ui/admin/dialogs/PermissionSettingsDialog.kt
ui/admin/dialogs/AddPermissionDialog.kt
```

Dialog requirements:

- all dialogs use `AdminDialogFrame`;
- content is scrollable where needed;
- match screenshots;
- buttons update local state or are no-op with correct visuals.

## Phase 6. Implement selected/expanded states

Important states from screenshots:

### User settings

- no selected role;
- select closed;
- select expanded;
- role selected and action buttons visible.

### Role settings

- top form visible;
- assigned permissions visible;
- scrolled bottom with actions visible.

### Permission settings

- form plus actions block.

Use previews to show these states.

## Phase 7. Add previews

Create previews for:

```text
AdminPanelUsersPreview
AdminPanelRolesPreview
AdminPanelPermissionsPreview
UserSettingsDialogPreview
UserSettingsDialogSelectOpenPreview
UserSettingsDialogSelectedRolePreview
RoleSettingsDialogPreview
AddRoleDialogPreview
AddPermissionDialogPreview
PermissionSettingsDialogPreview
```

Recommended preview size:

```kotlin
@Preview(widthDp = 382, heightDp = 825, showBackground = true)
```

If this size causes issues, use:

```kotlin
@Preview(widthDp = 390, heightDp = 844, showBackground = true)
```

## Phase 8. Connect to entry point carefully

If this is a demo task, update `MainActivity.kt` to show:

```kotlin
YsuAdminTheme {
    AdminPanelScreen()
}
```

If the project already has navigation, add the screen without breaking existing navigation.

## Phase 9. Build and fix

Run Gradle build or Android Studio build.

Fix:

- missing imports;
- Material Icons dependency issues;
- Compose API version differences;
- package name mismatches;
- preview errors.

## Acceptance checklist

The result is good when:

- app compiles;
- no XML layouts were created;
- main screen visually matches screenshots;
- dialogs visually match screenshots;
- search fields look correct;
- card radius and spacing are close;
- tabs are horizontally scrollable;
- add button appears only for `Роли` and `Права`;
- refresh button appears on all tabs;
- selected role state displays action buttons;
- select expanded state is implemented;
- all Russian texts match screenshots;
- there are previews for key screens.

## Do not overbuild

Since the user requested only design, avoid:

- real network layer;
- repositories;
- ViewModel complexity unless project architecture already requires it;
- authentication;
- database;
- backend integration.

Keep implementation clean, local and easy to replace with real data later.
